package org.infinispan.atomic;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.infinispan.Cache;
import org.infinispan.commons.marshall.jboss.GenericJBossMarshaller;
import org.infinispan.notifications.KeySpecificListener;
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
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
  * @author Pierre Sutra
 *  @since 6.0
 *
 */
@Listener(sync = false, clustered = true, primaryOnly = true)
public class AtomicObjectContainer extends KeySpecificListener {

    //
    // CLASS FIELDS
    //

    private static final MethodFilter mfilter = new MethodFilter() {
        @Override
        public boolean isHandled(Method m) {
            // ignore finalize()
            return !m.getName().equals("finalize");
        }
    };
    private static Log log = LogFactory.getLog(AtomicObjectContainer.class);
    private static final int CALL_TTIMEOUT_TIME = 3000;
    private static final int RETRIEVE_TTIMEOUT_TIME = 3000;

    //
    // OBJECT FIELDS
    //

    private Cache cache;
    private Object object;
    private Class clazz;
    private Object proxy;

    private ExecutorService callExecutors = Executors.newSingleThreadExecutor();

    private Boolean withReadOptimization; // serialize operation on the object copy
    private Method equalsMethod;
    private AtomicBoolean isInstalled;
    private Boolean isDisposed;
    private final AtomicObjectContainer listener = this;
    private Set<String> readOptimizationSuceedMethods;
    private Set<String> readOptimizationFailedMethods;

    private Map<Long,AtomicObjectCallFuture> registeredCalls;

    private AtomicObjectCallFuture retrieve_future;
    private ArrayList<AtomicObjectCallInvoke> retrieve_calls;
    private AtomicObjectCallRetrieve retrieve_call;

    public AtomicObjectContainer(final Cache c, final Class cl, final Object key, final boolean readOptimization, Method m, final boolean forceNew, final Object ... initArgs)
            throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException, InterruptedException, ExecutionException, NoSuchMethodException, InvocationTargetException {

        // check parameters
        assert !AtomicObject.class.isAssignableFrom(cl) || readOptimization==false;

        cache = c;
        clazz = cl;
        this.key = key;

        withReadOptimization = readOptimization;
        isInstalled = new AtomicBoolean(false);
        readOptimizationSuceedMethods = new ConcurrentSkipListSet<String>();
        readOptimizationFailedMethods = new ConcurrentSkipListSet<String>();

        equalsMethod = m;

        registeredCalls = new ConcurrentHashMap<Long, AtomicObjectCallFuture>();

        // build the proxy
        MethodHandler handler = new MethodHandler() {

            public Object invoke(Object self, Method m, Method proceed, Object[] args) throws Throwable {

                if (listener==null)
                    throw new IllegalAccessException();

                // 1 - local operation
                GenericJBossMarshaller marshaller = new GenericJBossMarshaller();
                if (withReadOptimization ) {
                    if (object==null)
                        initObject(forceNew, initArgs);
                    synchronized(listener){
                        if(readOptimizationSuceedMethods.contains(m.getName())){
                            return doCall(object,m.getName(),args);
                        }else if(!readOptimizationFailedMethods.contains(m.getName())){
                            Object copy = marshaller.objectFromByteBuffer(marshaller.objectToByteBuffer(object));
                            Object ret = doCall(copy,m.getName(),args);
                            if( equalsMethod == null ? copy.equals(object) : equalsMethod.invoke(copy, object).equals(Boolean.TRUE) ){
                                log.debug("Call "+m.getName()+"() local succeed");
                                readOptimizationSuceedMethods.add(m.getName());
                                return ret;
                            }else{
                                readOptimizationFailedMethods.add(m.getName());
                            }
                        }
                    }
                }

                // 2 - remote operation

                // 2.1 - call creation
                long callID = nextCallID();
                AtomicObjectCallInvoke invoke = new AtomicObjectCallInvoke(callID,m.getName(),args);
                byte[] bb = marshaller.objectToByteBuffer(invoke);
                AtomicObjectCallFuture future = new AtomicObjectCallFuture();
                registeredCalls.put(callID, future);
                log.debug("Call " + invoke);

                // 2.2 - listener installation and object creation (if necessary)
                if (!isInstalled.get() ) {
                    cache.addListener(listener);
                    initObject(forceNew,initArgs);
                    isInstalled.set(true);
                }

                // 2.3 - call execution
                cache.put(AtomicObjectContainer.this.key, bb);
                Object ret = future.get(CALL_TTIMEOUT_TIME,TimeUnit.MILLISECONDS);
                registeredCalls.remove(callID);
                if(!future.isDone()){
                    throw new TimeoutException("Unable to execute "+invoke+" on "+clazz+ " @ "+ AtomicObjectContainer.this.key);
                }
                log.debug("Return " + invoke+ " "+(ret==null ? "null" : ret.toString()));
                return ret;
            }
        };

        ProxyFactory fact = new ProxyFactory();
        fact.setSuperclass(clazz);
        fact.setFilter(mfilter);
        proxy = fact.createClass().newInstance();
        ((ProxyObject)proxy).setHandler(handler);

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
    public void onCacheModification(CacheEntryEvent event){

        log.debug(event.getType() + " " + event.isPre());

        if( !event.getKey().equals(key) )
            return;

        if(event.isPre())
            return;

        try {

            GenericJBossMarshaller marshaller = new GenericJBossMarshaller();
            byte[] bb = (byte[]) event.getValue();
            AtomicObjectCall call = (AtomicObjectCall) marshaller.objectFromByteBuffer(bb);
            log.debug("Receive " + call+" "+event.isOriginLocal());
            callExecutors.submit(new AtomicObjectContainerTask(call));

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public synchronized void dispose(boolean keepPersistent) throws IOException, InterruptedException, IllegalAccessException {

        log.debug("Disposing "+key+"["+clazz.getSimpleName()+"]");

        if (!registeredCalls.isEmpty())
            throw new IllegalAccessException();

        if (isInstalled.get()){
            cache.removeListener(listener);
            isInstalled.set(false);
        }

        if ( keepPersistent ) {
            log.debug(" ... persisted");
            GenericJBossMarshaller marshaller = new GenericJBossMarshaller();
            AtomicObjectCallPersist persist = new AtomicObjectCallPersist(0,object);
            byte[] bb = marshaller.objectToByteBuffer(persist);
            cache.put(key, bb);
        }

    }

    private void initObject(boolean forceNew, Object ... initArgs) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {

        if( !forceNew){
            GenericJBossMarshaller marshaller = new GenericJBossMarshaller();
            try {
                AtomicObjectCall persist = (AtomicObjectCall) marshaller.objectFromByteBuffer((byte[]) cache.get(key));
                if(persist instanceof AtomicObjectCallPersist){
                    log.debug("Persisted object "+key);
                    object = ((AtomicObjectCallPersist)persist).object;
                }else{
                    log.debug("Retrieving object "+key);
                    if(!isInstalled.get()){
                        // Register
                        cache.addListener(this);
                        isInstalled.set(true);
                    }
                    retrieve_future = new AtomicObjectCallFuture();
                    retrieve_call = new AtomicObjectCallRetrieve(nextCallID());
                    marshaller = new GenericJBossMarshaller();
                    cache.put(key,marshaller.objectToByteBuffer(retrieve_call));
                    retrieve_future.get(RETRIEVE_TTIMEOUT_TIME,TimeUnit.MILLISECONDS);
                    if(!retrieve_future.isDone()) throw new TimeoutException();
                }
                if (object instanceof AtomicObject){
                    ((AtomicObject)object).cache = this.cache;
                    ((AtomicObject)object).key = this.key;
                }
                return;
            } catch (Exception e) {
                log.debug("Unable to retrieve object " + key + " from the cache.");
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

        if (object instanceof AtomicObject){
            ((AtomicObject)object).cache = this.cache;
            ((AtomicObject)object).key = this.key;
        }

        if(found)
            log.debug("Object " + key + "[" + clazz.getSimpleName() + "] is created "+(object instanceof AtomicObject));
        else
            throw new IllegalArgumentException("Unable to find constructor for "+clazz.toString()+" with "+initArgs);

    }

    public Object getProxy(){
        return proxy;
    }

    public Class getClazz(){
        return clazz;
    }

    @Override
    public String toString(){
        return "Container ["+this.clazz.getSimpleName()+"::"+this.key.toString()+"]";
    }


    private static int containerSignature(Class clazz, Object key){
        return clazz.hashCode()+key.hashCode();
    }

    /**
     *
     * @param invocation
     * @return true if the operation is local
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private boolean handleInvocation(AtomicObjectCallInvoke invocation)
            throws InvocationTargetException, IllegalAccessException {
        synchronized(listener){
            Object ret = doCall(object,invocation.method,invocation.arguments);
            if(registeredCalls.containsKey(invocation.callID)){
                assert ! registeredCalls.get(invocation.callID).isDone() : "Received twice "+ invocation.callID+" ?";
                registeredCalls.get(invocation.callID).setReturnValue(ret);
                return true;
            }
            return false;
        }
    }

    //
    // HELPERS
    //

    private long nextCallID(){
        Random random = new Random(System.nanoTime());
        return Thread.currentThread().getId()*random.nextLong();
    }

    private Object doCall(Object obj, String method, Object[] args) throws InvocationTargetException, IllegalAccessException {
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

    //
    // INNER CLASSES
    //

    private class AtomicObjectContainerTask implements Callable<Integer>{

        private AtomicObjectCall call;

        public AtomicObjectContainerTask(AtomicObjectCall c){
            call = c;
        }

        @Override
        public Integer call() throws Exception {

            try {

                if (call instanceof AtomicObjectCallInvoke) {

                    if(object != null){

                        AtomicObjectCallInvoke invocation = (AtomicObjectCallInvoke) call;
                        handleInvocation(invocation);

                    }else if (retrieve_calls != null) {

                        retrieve_calls.add((AtomicObjectCallInvoke) call);

                    }

                } else if (call instanceof AtomicObjectCallRetrieve) {

                    if (object != null ) {

                        AtomicObjectCallPersist persist = new AtomicObjectCallPersist(0,object);
                        GenericJBossMarshaller marshaller = new GenericJBossMarshaller();
                        cache.putAsync(key, marshaller.objectToByteBuffer(persist));

                    }else if (retrieve_call != null && retrieve_call.callID == ((AtomicObjectCallRetrieve)call).callID) {

                        assert retrieve_calls == null;
                        retrieve_calls = new ArrayList<AtomicObjectCallInvoke>();

                    }

                } else { // AtomicObjectCallPersist

                    if (object == null && retrieve_calls != null)  {
                        object = ((AtomicObjectCallPersist)call).object;
                        for(AtomicObjectCallInvoke invocation : retrieve_calls){
                            handleInvocation(invocation);
                        }
                        retrieve_future.setReturnValue(null);
                    }
                }

            } catch (InterruptedException e) {
                return 0;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return 1;

        }
    }


}
