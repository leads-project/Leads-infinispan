package org.infinispan.atomic;

import org.infinispan.Cache;
import org.infinispan.InvalidCacheUsageException;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.infinispan.atomic.AtomicObjectContainer.AtomicObjectContainertSignature;


/**
 * @author Pierre Sutra
 * @since 6.0
 */
public class AtomicObjectFactory {

    //
    // CLASS FIELDS
    //
    private static Log log = LogFactory.getLog(AtomicObjectFactory.class);
    private static Map<Cache,AtomicObjectFactory> factories = new HashMap<Cache,AtomicObjectFactory>();
    public synchronized static AtomicObjectFactory forCache(Cache cache){
        if(!factories.containsKey(cache))
            factories.put(cache, new AtomicObjectFactory(cache));
        return factories.get(cache);

    }

    //
    // OBJECT FIELDS
    //
	private Cache cache;
	private Map<AtomicObjectContainer.AtomicObjectContainertSignature,AtomicObjectContainer> registeredContainers;
    private int maxSize;


    /**
     *
     * Returns an object factory built on top of cache <i>c</i> with a bounded amount <i>m</i> of
     * containers in it. Upon the removal of a container, the object is stored persistently in the cache.
     *
     * @param c it must be synchronous.and transactional (with autoCommit set to true, its default value).
     * @param m max amount of containers kept by this factory.
     * @throws InvalidCacheUsageException
     */
    public AtomicObjectFactory(Cache<Object, Object> c, int m) throws InvalidCacheUsageException{
        if( ! c.getCacheConfiguration().clustering().cacheMode().isSynchronous()
                || c.getCacheConfiguration().transaction().transactionMode() != TransactionMode.TRANSACTIONAL )
            throw new InvalidCacheUsageException("The cache must be synchronous and transactional.");
        cache = c;
        maxSize = m;
        registeredContainers= new LinkedHashMap<AtomicObjectContainer.AtomicObjectContainertSignature,AtomicObjectContainer>(){
            @Override
            protected boolean removeEldestEntry(java.util.Map.Entry<AtomicObjectContainer.AtomicObjectContainertSignature,AtomicObjectContainer> eldest) {
                if(maxSize!=0 && this.size() == maxSize){
                    try {
                        eldest.getValue().dispose(true);
                    } catch (IOException e) {
                        e.printStackTrace();  // TODO: Customise this generated block
                    } catch (InterruptedException e) {
                        e.printStackTrace();  // TODO: Customise this generated block
                    }
                    return true;
                }
                return false;
            }
        };
        log = LogFactory.getLog(this.getClass());
    }

    /**
     *
     * Return an AtomicObjectFactory built on top of cache <i>c</i>.
     *
     * @param c a cache,  it must be synchronous.and transactional (with autoCommit set to true, its default value).
     */
	public AtomicObjectFactory(Cache<Object, Object> c) throws InvalidCacheUsageException{
        this(c,0);
	}


    /**
     *
     * Returns an atomic object of class <i>clazz</i>.
     * The class of this object must be initially serializable, as well as all the parameters of its methods.
     * Furthermore the class must be deterministic.
     *
     * @param clazz a class object
     * @param key to use in order to store the object.
     * @return an object of the class <i>clazz</i>
     * @throws InvalidCacheUsageException
     */
 	public synchronized <T> T getInstanceOf(Class<T> clazz, Object key)
            throws InvalidCacheUsageException{
        return getInstanceOf(clazz, key, false);
	}

    /**
     *
     * Returns an object of class <i>clazz</i>.
     * The class of this object must be initially serializable, as well as all the parameters of its methods.
     * Furthermore the class must be deterministic.
     *
     * The object is atomic if <i>withReadOptimization</i> equals false; otherwise it is sequentially consistent..
     * In more details, if <i>withReadOptimization</i>  is set, every call to the object is first executed locally on a copy of the object, and in case
     * the call does not modify the state of the object, the value returned is the result of this tentative execution.
     *
     * @param clazz a class object
     * @param key the key to use in order to store the object.
     * @param withReadOptimization set the read optimization on/off.
     * @return an object of the class <i>clazz</i>
     * @throws InvalidCacheUsageException
     */
    public synchronized <T> T getInstanceOf(Class<T> clazz, Object key, boolean withReadOptimization)
            throws InvalidCacheUsageException{
        return getInstanceOf(clazz, key, withReadOptimization, null, true);
    }

    /**
     *
     * Returns an object of class <i>clazz</i>.
     * The class of this object must be initially serializable, as well as all the parameters of its methods.
     * Furthermore the class must be deterministic.
     *
     * The object is atomic if <i>withReadOptimization</i> equals false; otherwise it is sequentially consistent..
     * In more details, if <i>withReadOptimization</i>  is set, every call to the object is executed locally on a copy of the object, and in case
     * the call does not modify the state of the object, the value returned is the result of this tentative execution.
     * If the method <i>equalsMethod</i>  is not null, it overrides the default <i>clazz.equals()</i> when testing that the state of the object and
     * its copy are identical.
     *
     * @param clazz a class object
     * @param key the key to use in order to store the object.
     * @param withReadOptimization set the read optimization on/off.
     * @param equalsMethod overriding the default <i>clazz.equals()</i>.
     * @param forceNew force the creation of the object, even if it exists already in the cache
     * @return an object of the class <i>clazz</i>
     * @throws InvalidCacheUsageException
     */
    public synchronized <T> T getInstanceOf(Class<T> clazz, Object key, boolean withReadOptimization, Method equalsMethod, boolean forceNew, Object ... initArgs)
            throws InvalidCacheUsageException{

        if( !(clazz instanceof Serializable)){
            throw new InvalidCacheUsageException("The object must be serializable.");
        }

        AtomicObjectContainertSignature signature = new AtomicObjectContainertSignature(clazz,key);

        try{
            if(!registeredContainers.containsKey(signature)){
                AtomicObjectContainer container = new AtomicObjectContainer(cache, clazz, key, withReadOptimization, equalsMethod, forceNew,initArgs);
                registeredContainers.put(signature, container);
                log.debug("Registering cache "+signature);
            }
        } catch (Exception e){
            e.printStackTrace();
            throw new InvalidCacheUsageException(e.getCause());
        }

        return (T) registeredContainers.get(signature).getProxy();

    }

    /**
     * Remove the object stored at key from the local state.
     * If flag <i>keepPersistent</i> is set, a persistent copy of the current state of the object is stored in the cache.
     *
     * @param clazz a class object
     * @param key the key to use in order to store the object.
     * @param keepPersistent indicates that a persistent copy is stored in the cache or not.
     */
    public synchronized void disposeInstanceOf(Class clazz, Object key, boolean keepPersistent)
            throws IOException, InvalidCacheUsageException {
    	
        AtomicObjectContainertSignature signature = new AtomicObjectContainertSignature(clazz,key);
    	log.debug("Disposing instance from key:"+signature);
        AtomicObjectContainer container = registeredContainers.get(signature);

        if( container == null )
            throw new InvalidCacheUsageException("The object does not exist.");

        if( ! container.getClazz().equals(clazz) )
            throw new InvalidCacheUsageException("The object is not of the right class.");

        try{
            container.dispose(keepPersistent);
        }catch (Exception e){
            throw new InvalidCacheUsageException(e.getCause());
        }

        registeredContainers.remove(signature);

    }

    /**
     * @return a hash value of the order in which the calls on all the atomic objects built by this factory were executed.
     */
    @Override
    public int hashCode(){
        int ret = 0;
        for(AtomicObjectContainer c : registeredContainers.values())
            ret += c.hashCode();
        return ret;

    }

}
