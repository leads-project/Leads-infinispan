package org.infinispan.versioning.impl;

import org.infinispan.Cache;
import org.infinispan.versioning.utils.collections.ShardedTreeMap;
import org.infinispan.versioning.utils.version.Version;
import org.infinispan.versioning.utils.version.VersionGenerator;
import sun.reflect.generics.tree.Tree;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;


/**
 *
 * @author Pierre Sutra
 * @since 7.0
 */
public class VersionedCacheDummyImpl<K,V> extends VersionedCacheAbstractImpl<K,V> {

    public VersionedCacheDummyImpl(Cache cache,
                                  VersionGenerator generator, String cacheName) {
        super(cache, generator, cacheName);
    }


    @Override
    public boolean isEmpty() {
        return  true;
    }

    @Override
    public Set<K> keySet() {
        return delegate.keySet();
    }

    @Override
    protected SortedMap<Version, V> versionMapGet(K key) {
        TreeMap<Version,V> map = (TreeMap<Version, V>) delegate.get(key);
        if(map==null)
            return  new ShardedTreeMap<Version, V>();
        return map;
    }

    @Override
    protected void versionMapPut(K key, V value, Version version) {
        TreeMap<Version,V> treeMap = (TreeMap<Version, V>) delegate.get(key);
        if(treeMap==null)
            treeMap = new TreeMap<Version, V>();
        treeMap.put(version, value);
        delegate.put(key,treeMap);
    }

    @Override
    public void putAll(K key, Map<Version,V> map){
        TreeMap<Version,V> treeMap = (TreeMap<Version, V>) delegate.get(key);
        if(treeMap==null)
            treeMap = new TreeMap<Version, V>();
        treeMap.putAll(map);
        delegate.put(key,treeMap);
    }

    @Override
    public boolean containsKey(Object o) {
        return delegate.containsKey(o);
    }

}
