package org.infinispan.atomic;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.infinispan.Cache;
import org.infinispan.commons.marshall.jboss.GenericJBossMarshaller;
import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.KeyValueFilter;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryEvent;
import org.infinispan.util.concurrent.TimeoutException;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.io.IOException;
import java.io.Serializable;
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
@Listener(sync = true,clustered = true)
public class AtomicObjectContainer {

    //
    // CLASS FIELDS
    //

    private static final MethodFilter mfilter = new MethodFilter() {
        @Override
        public boolean isHandled(Method arg0) {
            return true;
        }
    };
    private static Log log = LogFactory.getLog(AtomicObjectContainer.class);
    private static final int CALL_TTIMEOUT_TIME = 3000;
    private static final int RETRIEVE_TTIMEOUT_TIME = 30000;
    private static ExecutorService callExecutors = Executors.newCachedThreadPool();
    private static Random random = new Random(System.currentTimeMillis());

    //
    // OBJECT FIELDS
    //

    private Cache cache;
    private Object object;
    private Class clazz;
    private Object key;
    private Object proxy;

    private Boolean withReadOptimization;
    private Set<String> readOptimizationFailedMethods;
    private Set<String> readOptimizationSuceedMethods;
    private Method equalsMethod;

    private Map<Integer,AtomicObjectCallFuture> registeredCalls;

    private AtomicObjectCallFuture retrieve_future;
    private ArrayList<AtomicObjectCallInvoke> retrieve_calls;
    private AtomicObjectCallRetrieve retrieve_call;

    public AtomicObjectContainer(Cache c, Class cl, Object k, boolean readOptimization, Method m, boolean forceNew, Object ... initArgs)
            throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException, InterruptedException, ExecutionException, NoSuchMethodException, InvocationTargetException {

        cache = c;
        clazz = cl;
        key = k;

        readOptimizationFailedMethods = new HashSet<String>();
        readOptimizationSuceedMethods = new HashSet<String>();
        withReadOptimization = readOptimization;

        equalsMethod = m;

        registeredCalls = new ConcurrentHashMap<Integer, AtomicObjectCallFuture>();

        // build the proxy
        MethodHandler handler = new MethodHandler() {
            public Object invoke(Object self, Method m, Method proceed, Object[] args) throws Throwable {

                GenericJBossMarshaller marshaller = new GenericJBossMarshaller();

                synchronized(withReadOptimization){
                    if (withReadOptimization){
                        if(readOptimizationSuceedMethods.contains(m.getName())){
                            return doCall(object,m.getName(),args);
                        }else if(!readOptimizationFailedMethods.contains(m.getName())){
                            Object copy = marshaller.objectFromByteBuffer(marshaller.objectToByteBuffer(object));
                            Object ret = doCall(copy,m.getName(),args);
                            if( equalsMethod == null ? copy.equals(object) : equalsMethod.invoke(copy, object).equals(Boolean.TRUE) ){
                                readOptimizationSuceedMethods.add(m.getName());
                                return ret;
                            }else{
                                readOptimizationFailedMethods.add(m.getName());
                            }
                        }
                    }
                }

                int callID = nextCallID(cache);
                AtomicObjectCallInvoke invoke = new AtomicObjectCallInvoke(callID,m.getName(),args);
                byte[] bb = marshaller.objectToByteBuffer(invoke);
                AtomicObjectCallFuture future = new AtomicObjectCallFuture();
                registeredCalls.put(callID, future);
                cache.put(key, bb);
                log.debug("Called " + invoke + " on object " + key);

                Object ret = future.get(CALL_TTIMEOUT_TIME,TimeUnit.MILLISECONDS);
                registeredCalls.remove(callID);
                if(!future.isDone()){
                    throw new TimeoutException("Unable to execute "+invoke+" on "+clazz+ " @ "+key);
                }
                return ret;
            }
        };

        ProxyFactory fact = new ProxyFactory();
        fact.setSuperclass(clazz);
        fact.setFilter(mfilter);
        proxy = fact.createClass().newInstance();
        ((ProxyObject)proxy).setHandler(handler);

        // Register
        cache.addListener(this, new AtomicObjectContainterFilter(key), null);

        // Build the object
        initObject(forceNew, initArgs);

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

        // System.out.println("receive "+event.getValue()+" with "+event.isPre());

        if(event.isPre())
            return;

        try {

            GenericJBossMarshaller marshaller = new GenericJBossMarshaller();
            byte[] bb = (byte[]) event.getValue();
            AtomicObjectCall call = (AtomicObjectCall) marshaller.objectFromByteBuffer(bb);
            log.debug("Received " + call + " from " + event.getCache().toString());
            callExecutors.submit(new AtomicObjectContainerTask(call));

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void dispose(boolean keepPersistent)
            throws IOException, InterruptedException {
        if (!keepPersistent) {
            cache.remove(key);
        } else {
            GenericJBossMarshaller marshaller = new GenericJBossMarshaller();
            AtomicObjectCallPersist persist = new AtomicObjectCallPersist(0,object);
            cache.putAsync(key,marshaller.objectToByteBuffer(persist));
        }
        cache.removeListener(this);
    }

    private void initObject(boolean forceNew, Object ... initArgs) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {

        if( !forceNew){
            GenericJBossMarshaller marshaller = new GenericJBossMarshaller();
            try {
                AtomicObjectCall persist = (AtomicObjectCall) marshaller.objectFromByteBuffer((byte[]) cache.get(key));
                if(persist instanceof AtomicObjectCallPersist){
                    object = ((AtomicObjectCallPersist)persist).object;
                }else{
                    log.debug("Retrieving object "+key);
                    retrieve_future = new AtomicObjectCallFuture();
                    retrieve_call = new AtomicObjectCallRetrieve(nextCallID(cache));
                    marshaller = new GenericJBossMarshaller();
                    cache.put(key,marshaller.objectToByteBuffer(retrieve_call));
                    retrieve_future.get(RETRIEVE_TTIMEOUT_TIME,TimeUnit.MILLISECONDS);
                    if(!retrieve_future.isDone()) throw new TimeoutException();
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

        if(found)
            log.debug("Object "+key+" is created.");
        else
            throw new IllegalArgumentException("Unable to find constructor for "+clazz.toString()+" with "+initArgs);

    }

    public Object getProxy(){
        return proxy;
    }

    public Class getClazz(){
        return clazz;
    }

    /**
     * @return a hash of the order in which the calls where executed.
     */
    @Override
    public int hashCode(){
        return containerSignature(this.clazz,this.key);
    }

    @Override
    public String toString(){
        return "Container ["+this.clazz.toString()+"::"+this.key.toString()+"]";
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
        synchronized(withReadOptimization){
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

    private synchronized static int nextCallID(Cache c){
        int rand = random.nextInt();
        return rand+c.hashCode();
    }

    private static Object doCall(Object obj, String method, Object[] args) throws InvocationTargetException, IllegalAccessException {
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
                        cache.put(key, marshaller.objectToByteBuffer(persist));

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


    public static class AtomicObjectContainerSignature{

        int hash;

        public AtomicObjectContainerSignature(Class clazz, Object key){
            hash = clazz.hashCode() + key.hashCode();
        }

        public AtomicObjectContainerSignature(AtomicObjectContainer container){
            hash = container.clazz.hashCode() + container.key.hashCode();
        }

        @Override
        public int hashCode(){
            return hash;
        }

        @Override
        public boolean equals(Object o){
            if (!(o instanceof AtomicObjectContainerSignature))
                return false;
            return ((AtomicObjectContainerSignature)o).hash == this.hash;
        }

    }

    public static class AtomicObjectContainterFilter implements KeyValueFilter<Object,Object>, Serializable{

        private Object key;

        public AtomicObjectContainterFilter(Object k){
            this.key = k;
        }

        @Override
        public boolean accept(Object k, Object v, Metadata m) {
            return key.equals(k);
        }
    }

}
