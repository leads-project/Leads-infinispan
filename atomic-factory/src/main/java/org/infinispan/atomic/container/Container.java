package org.infinispan.atomic.container;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.infinispan.Cache;
import org.infinispan.atomic.Updatable;
import org.infinispan.commons.marshall.jboss.GenericJBossMarshaller;
import org.infinispan.executors.SerialExecutor;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryEvent;
import org.infinispan.util.concurrent.TimeoutException;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Pierre Sutra
 *  @since 6.0
 *
 */
@Listener(sync = true, clustered = true)
public class Container {

   //
   // CLASS FIELDS
   //

   private static final MethodFilter methodFilter = new MethodFilter() {
      @Override
      public boolean isHandled(Method m) {
         // ignore finalize() and externalization related methods.
         return !m.getName().equals("finalize")
               && !m.getName().equals("readExternal")
               && !m.getName().equals("writeExternal");
      }
   };
   private static Log log = LogFactory.getLog(Container.class);
   private static final int CALL_TTIMEOUT_TIME = 1000;
   private static final int RETRIEVE_TTIMEOUT_TIME = 1000;
   private static final int MAX_ENTRIES = 10000; // due to event singularity
   private static final String SEPARATOR = "#";
   private static Executor globalExecutors = Executors.newSingleThreadExecutor();

   //
   // OBJECT FIELDS
   //

   private Cache cache;
   private Object object;
   private Class clazz;
   private Object proxy;
   private List<String> updateMethods;
   private Executor callExecutor = new SerialExecutor(globalExecutors);

   private Boolean withReadOptimization; // serialize operation on the object copy
   private volatile int listenerState=0; // 0 = not installed, 1 = installed, -1 = disposed
   private final Container listener = this;

   private Call lastCall;
   private Map<UUID,CallFuture> registeredCalls;
   private CallFuture retrieveFuture;
   private ArrayList<CallInvoke> pendingCalls;
   private CallRetrieve retrieveCall;
   private Object key;
   
   private Map<UUID,Integer> receivedCalls;
   private AtomicInteger checksum;
   
   //
   // PUBLIC METHODS
   //

   public Container(final Cache c, final Class cl, final Object k, final boolean readOptimization,
         final boolean forceNew, final List<String> methods, final Object... initArgs)
         throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException, 
         InterruptedException, ExecutionException, NoSuchMethodException, InvocationTargetException {

      cache = c;
      clazz = cl;
      key = clazz.getSimpleName()+SEPARATOR+k.toString(); // to avoid collisions
      receivedCalls = new LinkedHashMap<UUID, Integer>(MAX_ENTRIES + 1, .75F, false) {
         protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > MAX_ENTRIES;
         }
      };
      checksum = new AtomicInteger(0);
      
      
      log.debug(this+"Opening.");
      updateMethods = methods;
      withReadOptimization = readOptimization;
      
      if (listenerState!=0)
         throw new IllegalAccessException("A container is a one shot object.");

      registeredCalls = new ConcurrentHashMap<>();
      
      // build the proxy
      MethodHandler handler = new MethodHandler() {

         public Object invoke(Object self, Method m, Method proceed, Object[] args) throws Throwable {

            GenericJBossMarshaller marshaller = new GenericJBossMarshaller();

            // 1 - local operation
            if ( withReadOptimization
                  && ! updateMethods.contains(m.getName()) ) {
               log.debug(this+"executing "+m.getName()+" locally");
               return callObject(object, m.getName(), args);
            }

            // 2 - remote operation

            // 2.1 - if necessary, listener installation and object re-creation
            initObject(true, forceNew, initArgs);

            // 2.2 - call creation
            UUID callID = nextCallID();
            CallInvoke invoke = new CallInvoke(callID,m.getName(),args);
            byte[] bb = marshaller.objectToByteBuffer(invoke);
            CallFuture future = new CallFuture();
            registeredCalls.put(callID, future);

            // 2.3 - call execution
            cache.put(Container.this.key, bb);
            log.debug(this+"Waiting on "+future);
            Object ret = future.get(CALL_TTIMEOUT_TIME,TimeUnit.MILLISECONDS);
            registeredCalls.remove(callID);
            if(!future.isDone()){
               throw new TimeoutException("Unable to execute "+invoke+" on "+clazz+ " @ "+ Container.this.key);
            }
            log.debug(this+"Return " + invoke+ " "+(ret==null ? "null" : ret.toString()));
            return ret;
         }
      };

      ProxyFactory fact = new ProxyFactory();
      fact.setSuperclass(clazz);
      fact.setFilter(methodFilter);
      proxy = fact.createClass().newInstance();
      ((ProxyObject)proxy).setHandler(handler);
      initObject(true, forceNew, initArgs);

      log.debug(this+"Opened.");

   }

   /**
    * Internal use of the listener API.
    *
    * @param event of class CacheEntryModifiedEvent
    */
   @SuppressWarnings({ "rawtypes", "unchecked" })
   @CacheEntryModified
   @CacheEntryCreated
   @Deprecated
   public synchronized void onCacheModification(CacheEntryEvent event){
      
      if( !event.getKey().equals(key) )
         return;

      if(event.isPre())
         return;
      
      log.debug(this + "Event received (" + event.getType() +")");
      
      try {

         GenericJBossMarshaller marshaller = new GenericJBossMarshaller();
         byte[] bb = (byte[]) event.getValue();
         Call call = (Call) marshaller.objectFromByteBuffer(bb);
         if (receivedCalls.containsKey(call.callID))
            return;
         checksum.addAndGet(call.hashCode());
         receivedCalls.put(call.callID,0);
         callExecutor.execute(new AtomicObjectContainerTask(call));

      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   public void dispose(boolean keepPersistent) throws IOException, InterruptedException{

      log.debug(this+"Disposing.");

      if (!registeredCalls.isEmpty()){
         log.warn("Cannot dispose "+this+"- registeredCalls non-empty");
         return;
      }

      if (listenerState==1) {
         if (keepPersistent) {
            byte[] last = null;
            byte[] bb;
            UUID callID = nextCallID();
            while (true) {
               bb = buildMarshalledCallPersist(callID);
               if (!java.util.Arrays.equals(bb,last)) {
                  cache.put(key, bb);
                  last = bb;
               } else {
                  break;
               }
            }
         }
         cache.removeListener(listener);
      }
      listenerState = -1;

      log.debug(this+"Disposed.");

   }

   public Object getProxy(){
      return proxy;
   }

   public Class getClazz(){
      return clazz;
   }

   public int checksum(){
      return this.checksum.intValue();
   }
   
   @Override
   public String toString(){
      return "Container ["+this.key.toString()+"]";
   }

   //
   // PRIVATE METHODS
   //

   /**
    *
    * @param invocation
    * @return true if the operation is local
    * @throws InvocationTargetException
    * @throws IllegalAccessException
    */
   private boolean handleInvocation(CallInvoke invocation)
         throws InvocationTargetException, IllegalAccessException {
      Object ret = callObject(object, invocation.method, invocation.arguments);
      CallFuture future = registeredCalls.get(invocation.callID);
      log.debug(this+"Calling " + invocation+" (isLocal="+(future!=null ? "true":"false")+")");
      if(future!=null){
         future.setReturnValue(ret);
         return true;
      }
      return false;
   }

   private void initObject(boolean installListener, boolean forceNew, Object... initArgs) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {

      if (object!=null && (!installListener || listenerState==1))
         return;

      if (installListener)
         installListener();

      if( !forceNew){
         GenericJBossMarshaller marshaller = new GenericJBossMarshaller();
         try {
            Call persist = (Call) marshaller.objectFromByteBuffer((byte[]) cache.get(key));
            
            if(persist instanceof CallPersist){
               log.debug(this+"Persisted object "+key);
               object = ((CallPersist)persist).object;
            }else{
               installListener();
               log.debug(this + "Retrieving object " + key);
               retrieveFuture = new CallFuture();
               retrieveCall = new CallRetrieve(nextCallID());
               marshaller = new GenericJBossMarshaller();
               pendingCalls = new ArrayList<>();
               cache.put(key, marshaller.objectToByteBuffer(retrieveCall));
               retrieveFuture.get(RETRIEVE_TTIMEOUT_TIME, TimeUnit.MILLISECONDS);
               if(!retrieveFuture.isDone()) throw new TimeoutException();
               log.debug(this+"Object "+key+" retrieved");
            }

            if (object instanceof Updatable){
               ((Updatable)object).setCache(this.cache);
               ((Updatable)object).setKey(this.key);
            }

            assert object!=null;
            return;
         } catch (Exception e) {
            log.debug(this+"Unable to retrieve object " + key + " from the cache ("+e.getMessage()+").");
         }
      }

      boolean found=false;
      Constructor[] allConstructors = clazz.getDeclaredConstructors();
      for (Constructor ctor : allConstructors) {
         Class<?>[] pType  = ctor.getParameterTypes();
         if(pType.length==initArgs.length){
            found=true;
            for (int i = 0; i < pType.length; i++) {
               if(!pType[i].isAssignableFrom(initArgs[i].getClass())){
                  found=false;
                  break;
               }
            }
            if(found){
               object = ctor.newInstance(initArgs);
               break;
            }
         }
      }

      if (object instanceof Updatable){
         ((Updatable)object).setCache(this.cache);
         ((Updatable)object).setKey(this.key);
      }

      if(found)
         log.debug(this+"Object " + key + "[" + clazz.getSimpleName() + "] is created (Updatable="+(object instanceof Updatable)+")");
      else
         throw new IllegalArgumentException("Unable to find constructor for "+clazz.toString()+" with "+initArgs);

      assert object!=null;
      
   }

   //
   // HELPERS
   //

   private void installListener(){
      if (listenerState==1)
         return;
      log.debug(this+"Installing listener "+key);
      cache.addListener(listener);
      listenerState = 1;
   }

   private UUID nextCallID(){
      UUID ret = UUID.randomUUID();
      log.trace(this + "next CallID " + ret);
      return ret;
   }

   private synchronized Object callObject(Object obj, String method, Object[] args) throws InvocationTargetException, IllegalAccessException {
      log.trace(this+"Calling "+method.toString()+"()");
      boolean isFound = false;
      Object ret = null;
      for (Method m : obj .getClass().getMethods()) { // only public methods (inherited and not)
         if (method.equals(m.getName())) {
            boolean isAssignable = true;
            Class[] argsTypes = m.getParameterTypes();
            if(argsTypes.length == args.length){
               for(int i=0; i<argsTypes.length; i++){
                  if( !argsTypes[i].isAssignableFrom(args[i].getClass()) ){
                     isAssignable = false;
                     break;
                  }
               }
            }else{
               isAssignable = false;
            }
            if(!isAssignable)
               continue;

            ret = m.invoke(obj, args);
            isFound = true;
            break;
         }
      }

      if(!isFound)
         throw new IllegalStateException("Method "+method+" not found.");

      return ret;
   }
   
   private synchronized byte[] buildMarshalledCallPersist(UUID id) throws IOException, InterruptedException {
      GenericJBossMarshaller marshaller = new GenericJBossMarshaller();
      CallPersist persist = new CallPersist(id, object);
      return marshaller.objectToByteBuffer(persist);
   }

   //
   // INNER CLASSES
   //

   private class AtomicObjectContainerTask implements Runnable{

      public Call call;

      public AtomicObjectContainerTask(Call c){
         call = c;
      }

      @Override
      public void run() {

         if (listenerState==-1)
            return; // object is disposed.
                                          
         try {

            if (call instanceof CallInvoke) {

               if(object != null){

                  CallInvoke invocation = (CallInvoke) call;
                  handleInvocation(invocation);

               }else if (pendingCalls != null) {

                  pendingCalls.add((CallInvoke) call);

               }

            } else if (call instanceof CallRetrieve) {

               if (object != null ) {

                  log.debug("sending persistent state");

                  CallPersist persist = new CallPersist(nextCallID(),object);
                  GenericJBossMarshaller marshaller = new GenericJBossMarshaller();
                  cache.put(key, marshaller.objectToByteBuffer(persist));

               }else if (retrieveCall != null && retrieveCall.callID == ((CallRetrieve)call).callID) {

                  assert pendingCalls == null;
                  pendingCalls = new ArrayList<CallInvoke>();

               }

            } else {

               assert (call instanceof CallPersist);

               log.debug(this+"Persistent state received");

               if (object == null) {
                  if (retrieveFuture != null) {
                     object = ((CallPersist) call).object;
                     assert object != null;
                     if (pendingCalls != null) {
                        log.debug(this + "Applying pending calls");
                        for (CallInvoke invocation : pendingCalls) {
                           handleInvocation(invocation);
                        }
                        pendingCalls = null;
                     }
                     retrieveFuture.setReturnValue(null);
                  }
               }
               
            }
            
            lastCall = call;

         } catch (Exception e) {
            e.printStackTrace();
         }

      }
   }

}
