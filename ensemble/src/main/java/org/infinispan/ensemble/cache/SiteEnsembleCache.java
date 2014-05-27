package org.infinispan.ensemble.cache;

import org.infinispan.client.hotrod.*;
import org.infinispan.commons.util.concurrent.NotifyingFuture;

import java.util.Collections;
import java.util.Map;

/**
 *
 * @author Pierre Sutra
 * @since 7.0
 */
public class SiteEnsembleCache<K,V> extends EnsembleCache<K,V> implements RemoteCache<K,V>{

    RemoteCache<K,V> delegate;

    public SiteEnsembleCache(String name, RemoteCache remoteCache) {
        super(name, Collections.EMPTY_LIST);
        delegate = remoteCache;
    }

    @Override
    public NotifyingFuture<V> putAsync(K key, V value) {
        return delegate.putAsync(key, value);
    }

    @Override
    public boolean containsKey(Object k) {
        return delegate.containsKey(k);
    }


    @Override
    public boolean removeWithVersion(K key, long version) {
        return delegate.removeWithVersion(key, version);
    }

    @Override
    public NotifyingFuture<Boolean> removeWithVersionAsync(K key, long version) {
        return delegate.removeWithVersionAsync(key, version);
    }

    @Override
    public boolean replaceWithVersion(K key, V newValue, long version) {
        return delegate.replaceWithVersion(key, newValue, version);
    }

    @Override
    public boolean replaceWithVersion(K key, V newValue, long version, int lifespanSeconds) {
        return delegate.replaceWithVersion(key, newValue, version, lifespanSeconds);
    }

    @Override
    public boolean replaceWithVersion(K key, V newValue, long version, int lifespanSeconds, int maxIdleTimeSeconds) {
        return delegate.replaceWithVersion(key, newValue, version, lifespanSeconds, maxIdleTimeSeconds);
    }

    @Override
    public NotifyingFuture<Boolean> replaceWithVersionAsync(K key, V newValue, long version) {
        return delegate.replaceWithVersionAsync(key, newValue, version);
    }

    @Override
    public NotifyingFuture<Boolean> replaceWithVersionAsync(K key, V newValue, long version, int lifespanSeconds) {
        return delegate.replaceWithVersionAsync(key, newValue, version, lifespanSeconds);
    }

    @Override
    public NotifyingFuture<Boolean> replaceWithVersionAsync(K key, V newValue, long version, int lifespanSeconds, int maxIdleSeconds) {
        return delegate.replaceWithVersionAsync(key, newValue, version, lifespanSeconds, maxIdleSeconds);
    }

    @Override
    public VersionedValue<V> getVersioned(K key) {
        return delegate.getVersioned(key);
    }

    @Override
    public NotifyingFuture<VersionedValue<V>> getVersionedAsynch(K key) {
        return delegate.getVersionedAsynch(key);
    }

    @Override
    public MetadataValue<V> getWithMetadata(K key) {
        return delegate.getWithMetadata(key);
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
    public ServerStatistics stats() {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public RemoteCache<K, V> withFlags(Flag... flags) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public RemoteCacheManager getRemoteCacheManager() {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public Map<K, V> getBulk() {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public Map<K, V> getBulk(int size) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public String getProtocolVersion() {
        return null;  // TODO: Customise this generated block
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
