package org.infinispan.versioning.impl;

import org.infinispan.Cache;
import org.infinispan.atomic.AtomicObjectFactory;
import org.infinispan.versioning.utils.version.Version;
import org.infinispan.versioning.utils.version.VersionGenerator;
import org.jboss.logging.Logger;

import java.util.*;

/**
 *
 * @author Pierre Sutra
 * @since 7.0
 */
public class VersionedCacheAtomicMapImpl<K,V> extends VersionedCacheAbstractImpl<K,V> {

    AtomicObjectFactory factory;
    Logger logger;
    public VersionedCacheAtomicMapImpl(Cache delegate, VersionGenerator generator, String name) {
        super(delegate,generator,name);
        factory = new AtomicObjectFactory((Cache<Object, Object>) delegate);
        this.logger  = Logger.getLogger(this.getClass());

    }

    @Override
    protected SortedMap<Version, V> versionMapGet(K key) {
        HashMap<Version,V> map = factory.getInstanceOf(HashMap.class,key,true,null,false);
        return new TreeMap<Version,V>(map);
    }

    @Override
    protected void versionMapPut(K key, V value, Version version) {
        factory.getInstanceOf(HashMap.class,key,true,null,false).put(version, value);
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
            if(factory.getInstanceOf(HashMap.class,k,true,null,false).containsValue(o))
                return true;
        }
        return false;
    }

    @Override
    public Set<K> keySet() {
        return delegate.keySet();
    }

    @Override
    public Collection<V> get(K key, Version first, Version last) {
        HashMap<Version,V> map = factory.getInstanceOf(HashMap.class,key,true,null,false);
        TreeMap<Version,V> treeMap = new TreeMap<Version,V>(map);
        return treeMap.subMap(first, last).values();
    }

    @Override
    public void putAll(K key, Map<Version,V> map){
        factory.getInstanceOf(HashMap.class,key,true,null,false).putAll(map);
    }

}

