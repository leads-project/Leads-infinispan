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
    protected SortedMap<Version, String> versionMapGet(K key) {
                ShardedTreeMap<Version,String> treeMap = factory.getInstanceOf(ShardedTreeMap.class,key,true,null,false);
        factory.disposeInstanceOf(ShardedTreeMap.class, key, true);
        return treeMap;
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


//    @Override
//    protected SortedMap<Version, V> versionMapGet(K key) {
//        return factory.getInstanceOf(ShardedTreeMap.class,key,true,null,false);
//    }
//
//    @Override
//    protected void versionMapPut(K key, V value, Version version) {
//        factory.getInstanceOf(ShardedTreeMap.class,key,true,null,false).put(version, value);
//    }
//
//    @Override
//    public boolean isEmpty() {
//        return delegate.isEmpty();
//    }
//
//    @Override
//    public boolean containsKey(Object o) {
//        return delegate.containsKey(o);
//    }
//
//    @Override
//    public boolean containsValue(Object o) {
//        for(Object k: keySet()){
//            if(factory.getInstanceOf(ShardedTreeMap.class, k, true, null, false).containsValue(o))
//                return true;
//        }
//        return false;
//    }
//
//    @Override
//    public Set<K> keySet() {
//        return delegate.keySet();
//    }
//
//    @Override
//    public V get(Object key){
//        ShardedTreeMap<Version,V> shardedMap= factory.getInstanceOf(ShardedTreeMap.class,key,true,null,false);
//        V ret = shardedMap.get(key);
//        factory.disposeInstanceOf(ShardedTreeMap.class,key,true);
//        return ret;
//    }
//
//    @Override
//    public V put(K key, V value) {
//        ShardedTreeMap<Version,V> shardedMap= factory.getInstanceOf(ShardedTreeMap.class,key,true,null,false);
//        if (shardedMap.isEmpty()){
//            shardedMap.put(generator.generateNew(),value);
//        }else{
//            Version last = shardedMap.lastKey();
//            Version next = generator.increment(last);
//            shardedMap.put(next,value);
//        }
//        factory.disposeInstanceOf(ShardedTreeMap.class,key,true);
//        return null;
//    }
//
//
//    @Override
//    public Collection<Version> get(K key, Version first, Version last) {
//        assert first != null && last != null;
//        ShardedTreeMap<Version,V> shardedMap= factory.getInstanceOf(ShardedTreeMap.class,key,true,null,false);
//        Collection<Version> ret = shardedMap.subMap(first,last).keySet();
//        factory.disposeInstanceOf(ShardedTreeMap.class,key,true);
//        return ret;
//    }
//
//   @Override
//    public void putAll(K key, Map<Version,V> map){
//        ShardedTreeMap<Version,V> shardedMap= factory.getInstanceOf(ShardedTreeMap.class,key,true,null,false);
//        shardedMap.putAll(map);
//        factory.disposeInstanceOf(ShardedTreeMap.class,key,true);
//    }

}
