package org.infinispan.versioning.impl;

import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMapLookup;
import org.infinispan.container.versioning.IncrementableEntryVersion;
import org.infinispan.container.versioning.VersionGenerator;
import org.infinispan.versioning.utils.version.IncrementableEntryVersionComparator;

import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author Fábio André Coelho, Pierre Sutra
 * @since 4.0
 */
public class VersionedCacheAtomicMapImpl<K,V> extends VersionedCacheAbstractImpl<K,V> {

    public VersionedCacheAtomicMapImpl(Cache delegate, VersionGenerator generator, String name) {
        super(delegate,generator,name);
    }

    @Override
    protected SortedMap<IncrementableEntryVersion, V> versionMapGet(K key) {
        TreeMap map =  new TreeMap<IncrementableEntryVersion, V>(new IncrementableEntryVersionComparator());
        map.putAll(AtomicMapLookup.getAtomicMap(delegate, key));
        return map;
    }

    @Override
    protected void versionMapPut(K key, V value, IncrementableEntryVersion version) {
        AtomicMapLookup.getAtomicMap(delegate, key).put(version,value);
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
        for(K k: delegate.keySet()){
            if(AtomicMapLookup.getAtomicMap(delegate,k).containsValue(o))
                return true;
        }
        return false;
    }

    @Override
    public Set<K> keySet() {
        return delegate.keySet();
    }
}
