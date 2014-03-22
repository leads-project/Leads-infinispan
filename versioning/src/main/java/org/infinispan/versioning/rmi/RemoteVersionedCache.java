package org.infinispan.versioning.rmi;

import org.infinispan.commons.util.concurrent.NotifyingFuture;
import org.infinispan.versioning.utils.version.Version;


import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by pasin on 21/03/14.
 */
public interface RemoteVersionedCache<K,V> extends Remote {
    public String getName() throws RemoteException;
    public String getVersion() throws RemoteException;

    public void put(K key, V value, Version version) throws RemoteException;
    public V put(K key, V value) throws RemoteException;
    public V put(K k, V v, long l, TimeUnit timeUnit) throws RemoteException;
    public V put(K k, V v, long l, TimeUnit timeUnit, long l2, TimeUnit timeUnit2) throws RemoteException;

    public V putIfAbsent(K key, V value) throws RemoteException;
    public V putIfAbsent(K k, V v, long l, TimeUnit timeUnit) throws RemoteException;
    public V putIfAbsent(K k, V v, long l, TimeUnit timeUnit, long l2, TimeUnit timeUnit2) throws RemoteException;
    public void putAll(Map<? extends K, ? extends V> m) throws RemoteException;
    public void putAll(Map<? extends K, ? extends V> map, long l, TimeUnit timeUnit) throws RemoteException;
    public void putAll(Map<? extends K, ? extends V> map, long l, TimeUnit timeUnit, long l2, TimeUnit timeUnit2) throws RemoteException;
    public V replace(K key, V value) throws RemoteException;
    public V replace(K k, V v, long l, TimeUnit timeUnit) throws RemoteException;
    public V replace(K k, V v, long l, TimeUnit timeUnit, long l2, TimeUnit timeUnit2) throws RemoteException;
    public boolean replace(K key, V oldValue, V newValue) throws RemoteException;
    public boolean replace(K k, V v, V v2, long l, TimeUnit timeUnit) throws RemoteException;
    public boolean replace(K k, V v, V v2, long l, TimeUnit timeUnit, long l2, TimeUnit timeUnit2) throws RemoteException;

    public V get(K k) throws RemoteException;
    public V get(K key, Version version) throws RemoteException;
    public Collection<V> get(K key, Version first, Version last) throws RemoteException;
    public V getLatest(K key, Version upperBound) throws RemoteException;
    public V getEarliest(K key, Version lowerBound) throws RemoteException;
    public Version getLatestVersion(K key) throws RemoteException;
    public Version getLatestVersion(K key, Version upperBound) throws RemoteException;
    public Version getEarliestVersion(K key) throws RemoteException;
    public Version getEarliestVersion(K key, Version lowerBound) throws RemoteException;

    public V remove(K o) throws RemoteException;
    public boolean remove(K key, V value) throws RemoteException;

    public void clear() throws RemoteException;
    public int size() throws RemoteException;
    public boolean isEmpty() throws RemoteException;

    public Set<K> keySet() throws RemoteException;
    public Collection<V> values() throws RemoteException;
    public Set<Map.Entry<K, V>> entrySet() throws RemoteException;
    public boolean containsKey(K key) throws RemoteException;
    public boolean containsValue(V value) throws RemoteException;

    public NotifyingFuture<V> putAsync(K k, V v) throws RemoteException;
    public NotifyingFuture<V> putAsync(K k, V v, long l, TimeUnit timeUnit) throws RemoteException;
    public NotifyingFuture<V> putAsync(K k, V v, long l, TimeUnit timeUnit, long l2, TimeUnit timeUnit2) throws RemoteException;
    public NotifyingFuture<Void> putAllAsync(Map<? extends K, ? extends V> map) throws RemoteException;
    public NotifyingFuture<Void> putAllAsync(Map<? extends K, ? extends V> map, long l, TimeUnit timeUnit) throws RemoteException;
    public NotifyingFuture<Void> putAllAsync(Map<? extends K, ? extends V> map, long l, TimeUnit timeUnit, long l2, TimeUnit timeUnit2) throws RemoteException;
    public NotifyingFuture<Void> clearAsync() throws RemoteException;
    public NotifyingFuture<V> putIfAbsentAsync(K k, V v) throws RemoteException;
    public NotifyingFuture<V> putIfAbsentAsync(K k, V v, long l, TimeUnit timeUnit) throws RemoteException;
    public NotifyingFuture<V> putIfAbsentAsync(K k, V v, long l, TimeUnit timeUnit, long l2, TimeUnit timeUnit2) throws RemoteException;
    public NotifyingFuture<V> removeAsync(K o) throws RemoteException;
    public NotifyingFuture<Boolean> removeAsync(K k, V v) throws RemoteException;
    public NotifyingFuture<V> replaceAsync(K k, V v) throws RemoteException;
    public NotifyingFuture<V> replaceAsync(K k, V v, long l, TimeUnit timeUnit) throws RemoteException;
    public NotifyingFuture<V> replaceAsync(K k, V v, long l, TimeUnit timeUnit, long l2, TimeUnit timeUnit2) throws RemoteException;
    public NotifyingFuture<Boolean> replaceAsync(K k, V v, V v2) throws RemoteException;
    public NotifyingFuture<Boolean> replaceAsync(K k, V v, V v2, long l, TimeUnit timeUnit) throws RemoteException;
    public NotifyingFuture<Boolean> replaceAsync(K k, V v, V v2, long l, TimeUnit timeUnit, long l2, TimeUnit timeUnit2) throws RemoteException;
    public NotifyingFuture<V> getAsync(K k) throws RemoteException;

    public void start() throws RemoteException;
    public void stop() throws RemoteException;
}
