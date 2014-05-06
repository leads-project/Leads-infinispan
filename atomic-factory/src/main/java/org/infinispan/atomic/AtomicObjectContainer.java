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
import java.util.*;
import java.util.concurrent.*;

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
    private static final int CALL_TTIMEOUT_TIME = 10000;
    private static final int RETRIEVE_TTIMEOUT_TIME = 10000;
    private static final int MAXPOOL = 100;
    private static PriorityBlockingQueue<AtomicObjectContainerTask> queue = new PriorityBlockingQueue<AtomicObjectContainerTask>(MAXPOOL,new TaskComparator());
    private static ThreadPoolExecutor callExecutors = new ThreadPoolExecutor(30, MAXPOOL, MAXPOOL, TimeUnit.SECONDS, (PriorityBlockingQueue) queue);
    private static Set<String> readOptimizationSuceedMethods = new ConcurrentSkipListSet<String>();
    private static Set<String> readOptimizationFailedMethods = new ConcurrentSkipListSet<String>();

    //
    // OBJECT FIELDS
    //

    private Cache cache;
    private Object object;
    private Class clazz;
    private Object proxy;

    private Boolean withReadOptimization; // serialize operation on the object copy
    private Method equalsMethod;
    private Integer listenerState; // 0 = not installed, 1 = installed, -1 = disposed
    private final AtomicObjectContainer listener = this;

    private Map<Long,AtomicObjectCallFuture> registeredCalls;

    private AtomicObjectCallFuture retrieve_future;
    private ArrayList<AtomicObjectCallInvoke> retrieve_calls;
    private AtomicObjectCallRetrieve retrieve_call;

    //
    // PUBLIC METHODS
    //

    public AtomicObjectContainer(final Cache c, final Class cl, final Object key, final boolean readOptimization, Method m, final boolean forceNew, final Object ... initArgs)
            throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException, InterruptedException, ExecutionException, NoSuchMethodException, InvocationTargetException {

        // check parameters
        assert !AtomicObject.class.isAssignableFrom(cl) || readOptimization==false;

        cache = c;
        clazz = cl;
        this.key = key;

        withReadOptimization = readOptimization;
        listenerState = 0;
        equalsMethod = m;

        registeredCalls = new ConcurrentHashMap<Long, AtomicObjectCallFuture>();

        // build the proxy
        MethodHandler handler = new MethodHandler() {

            public Object invoke(Object self, Method m, Method proceed, Object[] args) throws Throwable {

                GenericJBossMarshaller marshaller = new GenericJBossMarshaller();

                if (listener==null)
                    throw new IllegalAccessException();

                // 1 - local operation
                if (withReadOptimization ) {
                    initObject(false, forceNew, initArgs);
                    if(readOptimizationSuceedMethods.contains(m.getName())){
                        return callObject(object, m.getName(), args);
                    }else if(!readOptimizationFailedMethods.contains(m.getName())){
                        Object copy = cloneObject(object);
                        Object ret = callObject(copy, m.getName(), args);
                        if( equalsMethod == null ? copy.equals(object) : equalsMethod.invoke(copy, object).equals(Boolean.TRUE) ){
                            log.debug("Call "+m.getName()+"() local succeed");
                            readOptimizationSuceedMethods.add(m.getName());
                            return ret;
                        }else{
                            readOptimizationFailedMethods.add(m.getName());
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
                initObject(true, forceNew, initArgs);

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
            log.debug("Receive " + call+" (isOriginLocal="+event.isOriginLocal()+")");
            callExecutors.submit(new AtomicObjectContainerTask(call,object));

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public synchronized void dispose(boolean keepPersistent) throws IOException, InterruptedException, IllegalAccessException {

        log.debug("Disposing "+key+"["+clazz.getSimpleName()+"]");

        if (!registeredCalls.isEmpty())
            throw new IllegalAccessException();

        if (listenerState==1)
            cache.removeListener(listener);
        listenerState = -1;

        if ( keepPersistent ) {
            log.debug(" ... persisted");
            GenericJBossMarshaller marshaller = new GenericJBossMarshaller();
            AtomicObjectCallPersist persist = new AtomicObjectCallPersist(0,object);
            byte[] bb = marshaller.objectToByteBuffer(persist);
            cache.put(key, bb);
        }

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
    private boolean handleInvocation(AtomicObjectCallInvoke invocation)
            throws InvocationTargetException, IllegalAccessException {
        synchronized(listener){
            Object ret = callObject(object, invocation.method, invocation.arguments);
            AtomicObjectCallFuture future = registeredCalls.get(invocation.callID);
            if(future!=null){
                future.setReturnValue(ret);
                return true;
            }
            return false;
        }
    }

    private synchronized void initObject(boolean installListener, boolean forceNew, Object... initArgs) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {

        if (object!=null && (!installListener || listenerState==1))
            return;

        if (installListener) {
            if (listenerState==1)
                return;
            if (listenerState==-1)
                throw new IllegalAccessException();
            cache.addListener(listener);
            listenerState = 1;
        }

        if( !forceNew){
            GenericJBossMarshaller marshaller = new GenericJBossMarshaller();
            try {
                AtomicObjectCall persist = (AtomicObjectCall) marshaller.objectFromByteBuffer((byte[]) cache.get(key));
                if(persist instanceof AtomicObjectCallPersist){
                    log.debug("Persisted object "+key);
                    object = ((AtomicObjectCallPersist)persist).object;
                }else{
                    log.debug("Retrieving object "+key);
                    if (installListener=false)
                        throw new IllegalAccessException();
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
            log.debug("Object " + key + "[" + clazz.getSimpleName() + "] is created (AtomicObject="+(object instanceof AtomicObject)+")");
        else
            throw new IllegalArgumentException("Unable to find constructor for "+clazz.toString()+" with "+initArgs);

    }

    //
    // HELPERS
    //

    private long nextCallID(){
        Random random = new Random(System.nanoTime());
        return Thread.currentThread().getId()*random.nextLong();
    }

    private synchronized Object callObject(Object obj, String method, Object[] args) throws InvocationTargetException, IllegalAccessException {
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

    private synchronized Object cloneObject(Object o) throws IOException, InterruptedException, ClassNotFoundException {
        GenericJBossMarshaller marshaller = new GenericJBossMarshaller();
        return marshaller.objectFromByteBuffer(marshaller.objectToByteBuffer(object));
    }

    //
    // INNER CLASSES
    //

    private class AtomicObjectContainerTask implements Callable<Integer>{

        private AtomicObjectCall call;
        private Object object;

        public AtomicObjectContainerTask(AtomicObjectCall c, Object o){
            call = c;
            object = o;
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

    private static class TaskComparator implements Comparator<AtomicObjectContainerTask> {
        public int compare(AtomicObjectContainerTask t1, AtomicObjectContainerTask t2){
            if (t1.equals(t2))
                return 0;
            if (t1.object.equals(t2.object))
                return 1;
            return -1;
        }
    }



}
