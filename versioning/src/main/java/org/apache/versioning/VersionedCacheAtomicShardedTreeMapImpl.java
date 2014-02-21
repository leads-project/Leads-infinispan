package org.apache.versioning;

import org.infinispan.Cache;
import org.infinispan.atomic.AtomicObjectFactory;
import org.infinispan.container.versioning.IncrementableEntryVersion;
import org.infinispan.container.versioning.VersionGenerator;

import java.util.Set;
import java.util.SortedMap;

/**
 * // TODO: Document this
 *
 * @author otrack
 * @since 4.0
 */
public class VersionedCacheAtomicShardedTreeMapImpl<K,V> extends VersionedCacheImpl<K,V> {

    AtomicObjectFactory factory;
    private EntryVersionShardedTreeMap delegate;


    public VersionedCacheAtomicShardedTreeMapImpl(Cache delegate, VersionGenerator generator, String name) {
        super(delegate,generator,name);
        factory = new AtomicObjectFactory((Cache<Object, Object>) delegate);
    }

    @Override
    protected SortedMap<IncrementableEntryVersion, V> versionMapGet(K key) {
        return factory.getInstanceOf(EntryVersionShardedTreeMap.class,key,true,null,false);
    }

    @Override
    protected void verionsMapPut(K key, V value, IncrementableEntryVersion version) {
        factory.getInstanceOf(EntryVersionShardedTreeMap.class,key,true,null,false).put(version, value);
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
