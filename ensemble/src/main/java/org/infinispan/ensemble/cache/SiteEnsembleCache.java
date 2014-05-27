package org.infinispan.ensemble.cache;

import org.infinispan.client.hotrod.RemoteCache;

import java.util.Collections;

/**
 *
 * @author Pierre Sutra
 * @since 7.0
 */
public class SiteEnsembleCache<K,V> extends EnsembleCache<K,V> {

    RemoteCache<K,V> delegate;

    public SiteEnsembleCache(String name, RemoteCache remoteCache) {
        super(name, Collections.EMPTY_LIST);
        delegate = remoteCache;
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public V get(Object o) {
        return delegate.get(o);
    }

    @Override
    public V put(K key, V value) {
        return delegate.put(key,value);
    }
}
