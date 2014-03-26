package org.infinispan.versioning.impl;

import org.infinispan.Cache;
import org.infinispan.atomic.AtomicObjectFactory;
import org.infinispan.versioning.utils.version.Version;
import org.infinispan.versioning.utils.version.VersionGenerator;
import org.jboss.logging.Logger;

import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * // TODO: Document this
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public class VersionedCacheAtomicTreeMapImpl<K,V> extends VersionedCacheAbstractImpl<K,V> {

    AtomicObjectFactory factory;
    Logger logger;
    public VersionedCacheAtomicTreeMapImpl(Cache delegate, VersionGenerator generator, String name) {
        super(delegate,generator,name);
        factory = new AtomicObjectFactory((Cache<Object, Object>) delegate);
        this.logger  = Logger.getLogger(this.getClass());

    }

    @Override
    protected SortedMap<Version, V> versionMapGet(K key) {
        return factory.getInstanceOf(TreeMap.class,key,true,null,true);
    }

    @Override
    protected void versionMapPut(K key, V value, Version version) {
    	long now=System.nanoTime();
        factory.getInstanceOf(TreeMap.class,key,true,null,true).put(version, value);
        long t=System.nanoTime()-now;
        logger.debug("PUT (ns) "+t);
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean containsKey(Object o) {
        return delegate.containsKey(o);
    }

    @Override
    public boolean containsValue(Object o) {
        for(Object k: delegate.keySet()){
            if(factory.getInstanceOf(TreeMap.class,k,true,null,true).containsValue(o))
                return true;
        }
        return false;
    }

    @Override
    public Set<K> keySet() {
        return delegate.keySet();
    }
}
