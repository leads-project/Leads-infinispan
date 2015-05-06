package org.infinispan.atomic.container.local;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.infinispan.Cache;
import org.infinispan.atomic.object.*;
import org.infinispan.atomic.Updatable;
import org.infinispan.atomic.container.AbstractContainer;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.marshall.jboss.GenericJBossMarshaller;
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

/**
 * @author Pierre Sutra
 *  @since 6.0
 *
 */
@Listener(sync = true, clustered = true)
public class OldLocalContainer extends AbstractContainer {

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
   private static Log log = LogFactory.getLog(OldLocalContainer.class);
   private static final int CALL_TTIMEOUT_TIME = 1000;
   private static final int RETRIEVE_TTIMEOUT_TIME = 1000;
   private static final int MAX_ENTRIES = 10000; // due to event singularity
   private static final String SEPARATOR = "#";
   private static Executor globalExecutors = Executors.newSingleThreadExecutor();

   //
   // OBJECT FIELDS
   //

   private Object object;
   private Executor callExecutor = new SerialExecutor(globalExecutors);
   private volatile int listenerState=0; // 0 = not installed, 1 = installed, -1 = disposed
   private final OldLocalContainer listener = this;

   private Call lastCall;
   private Map<UUID,CallFuture> registeredCalls;
   private CallFuture retrieveFuture;
   private ArrayList<CallInvoke> pendingCalls;
   private CallRetrieve retrieveCall;

   private Map<UUID,Integer> receivedCalls;

   //
   // PUBLIC METHODS
   //

   public OldLocalContainer(final BasicCache c, final Class cl, final Object k, final boolean readOptimization,
         final boolean forceNew, final List<String> methods, final Object... initArgs)
         throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException,
         InterruptedException, ExecutionException, NoSuchMethodException, InvocationTargetException {
      
      super(c,cl,k,readOptimization,forceNew,methods,methods,initArgs);

      receivedCalls = new LinkedHashMap<UUID, Integer>(MAX_ENTRIES + 1, .75F, false) {
         protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > MAX_ENTRIES;
         }
      };

      log.debug(this+"Opening.");

      if (listenerState!=0)
         throw new IllegalAccessException("A container is a one shot object.");

      registeredCalls = new ConcurrentHashMap<>();

      // build the proxy
      MethodHandler handler = new MethodHandler() {

         public Object invoke(Object self, Method m, Method proceed, Object[] args) throws Throwable {

            GenericJBossMarshaller marshaller = new GenericJBossMarshaller();

            // 1 - local operation
            if ( readOptimization
                  && ! methods.contains(m.getName()) ) {
               log.debug(this+"executing "+m.getName()+" locally");
               return callObject(object, m.getName(), args);
            }

            // 2 - remote operation

            // 2.1 - if necessary, listener installation and object re-creation
            initObject(true, forceNew, initArgs);

            // 2.2 - call creation
            CallInvoke invoke = new CallInvoke(listenerID,m.getName(),args);
            byte[] bb = marshaller.objectToByteBuffer(invoke);
            CallFuture future = new CallFuture(invoke.getCallID());
            registeredCalls.put(invoke.getCallID(), future);

            // 2.3 - call execution
            cache.put(key, bb);
            log.debug(this+"Waiting on "+future);
            Object ret = future.get(CALL_TTIMEOUT_TIME,TimeUnit.MILLISECONDS);
            registeredCalls.remove(invoke.getCallID());
            if(!future.isDone()){
               throw new TimeoutException("Unable to execute "+invoke+" on "+clazz+ " @ "+ OldLocalContainer.this.key);
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

   @Override 
   public void open()
         throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException, IOException {
      // TODO: Customise this generated block
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
         if (receivedCalls.containsKey(call.getCallID()))
            return;
         receivedCalls.put(call.getCallID(),0);
         callExecutor.execute(new AtomicObjectContainerTask(call));

      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   @Override
   public void close(boolean keepPersistent)
         throws InterruptedException, ExecutionException, TimeoutException, IOException {

      log.debug(this+"Closing.");

      if (!registeredCalls.isEmpty()){
         log.warn("Cannot close "+this+"- registeredCalls non-empty");
         return;
      }

      if (listenerState==1) {
         if (keepPersistent) {
            byte[] bb = buildMarshalledCallPersist();
            cache.put(key, bb);
         }
         ((Cache)cache).removeListener(listener);
      }
      listenerState = -1;

      log.debug(this+"Closed.");

   }

   @Override public void dispose() {
      // TODO: Customise this generated block
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
      CallFuture future = registeredCalls.get(invocation.getCallID());
      log.debug(this+"Calling " + invocation+" (isLocal="+(future!=null ? "true":"false")+")");
      if(future!=null){
         future.set(ret);
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
               object = ((CallPersist)persist).getBytes();
            }else{
               installListener();
               log.debug(this + "Retrieving object " + key);
               retrieveCall = new CallRetrieve(listenerID);
               retrieveFuture = new CallFuture(retrieveCall.getCallID());
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
      ((Cache)cache).addListener(listener);
      listenerState = 1;
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

   private synchronized byte[] buildMarshalledCallPersist() throws IOException, InterruptedException {
      GenericJBossMarshaller marshaller = new GenericJBossMarshaller();
      CallPersist persist = new CallPersist(listenerID, null, 0, marshaller.objectToByteBuffer(object));
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

                  GenericJBossMarshaller marshaller = new GenericJBossMarshaller();
                  CallPersist persist = new CallPersist(listenerID,null,0, marshaller.objectToByteBuffer(object));
                  cache.put(key, marshaller.objectToByteBuffer(persist));

               }else if (retrieveCall != null && retrieveCall.getCallID() == ((CallRetrieve)call).getCallID()) {

                  assert pendingCalls == null;
                  pendingCalls = new ArrayList<CallInvoke>();

               }

            } else {

               assert (call instanceof CallPersist);

               log.debug(this+"Persistent state received");

               if (object == null) {
                  if (retrieveFuture != null) {
                     object = ((CallPersist) call).getBytes();
                     assert object != null;
                     if (pendingCalls != null) {
                        log.debug(this + "Applying pending calls");
                        for (CallInvoke invocation : pendingCalls) {
                           handleInvocation(invocation);
                        }
                        pendingCalls = null;
                     }
                     retrieveFuture.set(null);
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
