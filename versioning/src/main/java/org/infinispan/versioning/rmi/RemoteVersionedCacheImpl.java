package org.infinispan.versioning.rmi;

import org.infinispan.commons.util.concurrent.NotifyingFuture;
import org.infinispan.versioning.VersionedCache;
import org.infinispan.versioning.utils.version.Version;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Marcelo Pasin (pasin)
 * @since 7.0
 */


public class RemoteVersionedCacheImpl<K,V> extends UnicastRemoteObject implements RemoteVersionedCache<K,V> {
    VersionedCache<K,V> delegate;

    public static final String SERVICE_NAME = "RemoteVersionedCacheService";

    RemoteVersionedCacheImpl(VersionedCache<K,V> delegate) throws RemoteException {
        super();
        this.delegate = delegate;
    }


    @Override
    public String getName() throws RemoteException {
        return delegate.getName();
    }

    @Override
    public String getVersion() throws RemoteException {
        return delegate.getVersion();
    }

    @Override
    public void put(K key, V value, Version version) throws RemoteException {
        delegate.put(key, value, version);
    }

    @Override
    public V put(K key, V value) throws RemoteException {
        return delegate.put(key, value);
    }

    @Override
    public V put(K k, V v, long l, TimeUnit timeUnit) throws RemoteException {
        return delegate.put(k, v, l, timeUnit);
    }

    @Override
    public V put(K k, V v, long l, TimeUnit timeUnit, long l2, TimeUnit timeUnit2) throws RemoteException {
        return delegate.put(k, v, l, timeUnit, l2, timeUnit2);
    }

    @Override
    public V putIfAbsent(K key, V value) throws RemoteException {
        return delegate.putIfAbsent(key, value);
    }

    @Override
    public V putIfAbsent(K k, V v, long l, TimeUnit timeUnit) throws RemoteException {
        return delegate.putIfAbsent(k, v, l, timeUnit);
    }

    @Override
    public V putIfAbsent(K k, V v, long l, TimeUnit timeUnit, long l2, TimeUnit timeUnit2) throws RemoteException {
        return delegate.putIfAbsent(k, v, l, timeUnit, l2, timeUnit2);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) throws RemoteException {
        delegate.putAll(m);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map, long l, TimeUnit timeUnit) throws RemoteException {
        delegate.putAll(map, l, timeUnit);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map, long l, TimeUnit timeUnit, long l2, TimeUnit timeUnit2) throws RemoteException {
        delegate.putAll(map, l, timeUnit, l2, timeUnit2);
    }

    @Override
    public V replace(K key, V value) throws RemoteException {
        return delegate.replace(key, value);
    }

    @Override
    public V replace(K k, V v, long l, TimeUnit timeUnit) throws RemoteException {
        return delegate.replace(k, v, l, timeUnit);
    }

    @Override
    public V replace(K k, V v, long l, TimeUnit timeUnit, long l2, TimeUnit timeUnit2) throws RemoteException {
        return delegate.replace(k, v, l, timeUnit, l2, timeUnit2);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) throws RemoteException {
        return delegate.replace(key, oldValue, newValue);
    }

    @Override
    public boolean replace(K k, V v, V v2, long l, TimeUnit timeUnit) throws RemoteException {
        return delegate.replace(k, v, v2, l, timeUnit);
    }

    @Override
    public boolean replace(K k, V v, V v2, long l, TimeUnit timeUnit, long l2, TimeUnit timeUnit2) throws RemoteException {
        return delegate.replace(k, v, v2, l, timeUnit, l2, timeUnit2);
    }

    @Override
    public V get(K k) throws RemoteException {
        return delegate.get(k);
    }

    @Override
    public V get(K key, Version version) throws RemoteException {
        return delegate.get(key, version);
    }

    @Override
    public Collection<V> get(K key, Version first, Version last) throws RemoteException {
        LinkedList<V> list = new LinkedList<V>();
        list.addAll(delegate.get(key, first, last));
        return list;
    }

    @Override
    public V getLatest(K key, Version upperBound) throws RemoteException {
        return delegate.getLatest(key, upperBound);
    }

    @Override
    public V getEarliest(K key, Version lowerBound) throws RemoteException {
        return delegate.getEarliest(key, lowerBound);
    }

    @Override
    public Version getLatestVersion(K key) throws RemoteException {
        return delegate.getLatestVersion(key);
    }

    @Override
    public Version getLatestVersion(K key, Version upperBound) throws RemoteException {
        return delegate.getLatestVersion(key, upperBound);
    }

    @Override
    public Version getEarliestVersion(K key) throws RemoteException {
        return delegate.getEarliestVersion(key);
    }

    @Override
    public Version getEarliestVersion(K key, Version lowerBound) throws RemoteException {
        return delegate.getEarliestVersion(key, lowerBound);
    }

    @Override
    public V remove(K o) throws RemoteException {
        return delegate.remove(o);
    }

    @Override
    public boolean remove(K key, V value) throws RemoteException {
        return delegate.remove(key, value);
    }

    @Override
    public void clear() throws RemoteException {
        delegate.clear();
    }

    @Override
    public int size() throws RemoteException {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() throws RemoteException {
        return delegate.isEmpty();
    }

    @Override
    public Set<K> keySet() throws RemoteException {
        return delegate.keySet();
    }

    @Override
    public Collection<V> values() throws RemoteException {
        return delegate.values();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() throws RemoteException {
        return delegate.entrySet();
    }

    @Override
    public boolean containsKey(K key) throws RemoteException {
        return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(V value) throws RemoteException {
        return delegate.containsValue(value);
    }

    @Override
    public NotifyingFuture<V> putAsync(K k, V v) throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<V> putAsync(K k, V v, long l, TimeUnit timeUnit) throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<V> putAsync(K k, V v, long l, TimeUnit timeUnit, long l2, TimeUnit timeUnit2) throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Void> putAllAsync(Map<? extends K, ? extends V> map) throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Void> putAllAsync(Map<? extends K, ? extends V> map, long l, TimeUnit timeUnit) throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Void> putAllAsync(Map<? extends K, ? extends V> map, long l, TimeUnit timeUnit, long l2, TimeUnit timeUnit2) throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Void> clearAsync() throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<V> putIfAbsentAsync(K k, V v) throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<V> putIfAbsentAsync(K k, V v, long l, TimeUnit timeUnit) throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<V> putIfAbsentAsync(K k, V v, long l, TimeUnit timeUnit, long l2, TimeUnit timeUnit2) throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<V> removeAsync(K o) throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Boolean> removeAsync(K k, V v) throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<V> replaceAsync(K k, V v) throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<V> replaceAsync(K k, V v, long l, TimeUnit timeUnit) throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<V> replaceAsync(K k, V v, long l, TimeUnit timeUnit, long l2, TimeUnit timeUnit2) throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Boolean> replaceAsync(K k, V v, V v2) throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Boolean> replaceAsync(K k, V v, V v2, long l, TimeUnit timeUnit) throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Boolean> replaceAsync(K k, V v, V v2, long l, TimeUnit timeUnit, long l2, TimeUnit timeUnit2) throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<V> getAsync(K k) throws RemoteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void start() throws RemoteException {
        delegate.start();
    }

    @Override
    public void stop() throws RemoteException {
        delegate.stop();
    }
}
