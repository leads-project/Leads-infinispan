package org.infinispan.versioning.impl;

import org.infinispan.Cache;
import org.infinispan.atomic.AtomicObjectFactory;
import org.infinispan.atomic.collections.ShardedTreeMap;
import org.infinispan.versioning.utils.version.Version;
import org.infinispan.versioning.utils.version.VersionGenerator;

import java.util.Map;
import java.util.SortedMap;

/**
 *
 * This class implements a sharded tree to store the versions.
 * More precisely, the map is a tree of trees, where the
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public class VersionedCacheShardedTreeMapImpl<K,V> extends VersionedCacheSplitAbstractImpl<K,V> {

    private AtomicObjectFactory factory;

    public VersionedCacheShardedTreeMapImpl(Cache delegate, VersionGenerator generator, String name) {
        super(delegate,generator,name);
        factory = new AtomicObjectFactory((Cache<Object, Object>) delegate);
    }

    @Override
    protected SortedMap<Version, String> versionMapGetStart(K key) {
        ShardedTreeMap<Version,String> treeMap = factory.getInstanceOf(ShardedTreeMap.class,key,true,null,false);
        return treeMap;
    }

    @Override
    protected void versionMapGetEnd(K key) {
        factory.disposeInstanceOf(ShardedTreeMap.class, key, true);
    }

    @Override
    protected void versionMapPut(K key, String value, Version version) {
        ShardedTreeMap<Version,String> treeMap = factory.getInstanceOf(ShardedTreeMap.class, key, true, null, false);
        treeMap.put(version, value);
        factory.disposeInstanceOf(ShardedTreeMap.class,key,true);
    }

    @Override
    protected void versionMapPutAll(K key, Map<Version, String> map) {
        ShardedTreeMap<Version,String> treeMap  = factory.getInstanceOf(ShardedTreeMap.class, key, true, null, false);
        treeMap.putAll(map);
        factory.disposeInstanceOf(ShardedTreeMap.class,key,true);
    }

}
