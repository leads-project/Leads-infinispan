package org.infinispan.versioning.impl;

import org.infinispan.Cache;
import org.infinispan.atomic.AtomicObjectFactory;
import org.infinispan.container.versioning.IncrementableEntryVersion;
import org.infinispan.container.versioning.VersionGenerator;
import org.infinispan.versioning.utils.version.EntryVersionShardedTreeMap;
import org.infinispan.versioning.utils.version.EntryVersionTreeMap;

import java.util.Set;
import java.util.SortedMap;

/**
 *
 * This class implements a sharded tree to store the versions.
 * More precisely, the map is a tree of trees, where the
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public class VersionedCacheAtomicShardedTreeMapImpl<K,V> extends VersionedCacheAbstractImpl<K,V> {

    private AtomicObjectFactory factory;

    public VersionedCacheAtomicShardedTreeMapImpl(Cache delegate, VersionGenerator generator, String name) {
        super(delegate,generator,name);
        factory = new AtomicObjectFactory((Cache<Object, Object>) delegate);

    }

    @Override
    protected SortedMap<IncrementableEntryVersion, V> versionMapGet(K key) {
        return factory.getInstanceOf(EntryVersionShardedTreeMap.class,key,true,null,false,factory,100);
    }

    @Override
    protected void versionMapPut(K key, V value, IncrementableEntryVersion version) {
        factory.getInstanceOf(EntryVersionShardedTreeMap.class,key,true,null,false,factory,100).put(version, value);
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
            if(factory.getInstanceOf(EntryVersionTreeMap.class, k, true, null, false).containsValue(o))
                return true;
        }
        return false;
    }

    @Override
    public Set<K> keySet() {
        return delegate.keySet();
    }

}
