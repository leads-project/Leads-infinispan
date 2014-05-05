package org.infinispan.versioning.impl;

import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMapLookup;
import org.infinispan.versioning.utils.version.Version;
import org.infinispan.versioning.utils.version.VersionGenerator;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author Fábio André Coelho, Pierre Sutra
 * @since 4.0
 */
public class VersionedCacheFinedGrainedHashMapImpl<K,V> extends VersionedCacheAbstractImpl<K,V> {
	
	Logger logger;
	
    public VersionedCacheFinedGrainedHashMapImpl(Cache delegate, VersionGenerator generator, String name) {
        super(delegate,generator,name);
        this.logger  = Logger.getLogger(this.getClass());
    }

    @Override
    protected SortedMap<Version, V> versionMapGet(K key) {
        TreeMap map =  new TreeMap<Version, V>();
        map.putAll(AtomicMapLookup.getFineGrainedAtomicMap(delegate, key));
        return map;
    }

    @Override
    protected void versionMapPut(K key, V value, Version version) {
    	long now=System.nanoTime();
        AtomicMapLookup.getFineGrainedAtomicMap(delegate, key).put(version,value);
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
            if(AtomicMapLookup.getFineGrainedAtomicMap(delegate,k).containsValue(o))
                return true;
        }
        return false;
    }

    @Override
    public Set<K> keySet() {
        return delegate.keySet();
    }

    @Override
    public void putAll(K k, Map<Version,V> map){
        AtomicMapLookup.getFineGrainedAtomicMap(delegate,k).putAll(map);
    }

    @Override
    public V get(Object o) {
        TreeMap<Version,V> map =  new TreeMap<Version, V>();
        map.putAll(AtomicMapLookup.getFineGrainedAtomicMap(delegate, (K)o));
        if(map.isEmpty()) return null;
        return map.get(map.lastKey());
    }

}
