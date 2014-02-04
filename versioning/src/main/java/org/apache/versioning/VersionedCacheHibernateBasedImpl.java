package org.apache.versioning;

import org.infinispan.commons.util.concurrent.NotifyingFuture;
import org.infinispan.container.versioning.IncrementableEntryVersion;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * // TODO: Document this
 *
 * @author otrack
 * @since 4.0
 */
public class VersionedCacheHibernateBasedImpl<K,V> implements VersionedCache<K,V> {


    @Override
    public void put(K key, V value, IncrementableEntryVersion version) {
        // TODO: Customise this generated block
    }

    @Override
    public Collection<V> get(K key, IncrementableEntryVersion first, IncrementableEntryVersion last) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public V get(K key, IncrementableEntryVersion version) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public String getName() {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public String getVersion() {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public V put(K key, V value) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public V put(K key, V value, long lifespan, TimeUnit unit) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public V putIfAbsent(K key, V value, long lifespan, TimeUnit unit) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map, long lifespan, TimeUnit unit) {
        // TODO: Customise this generated block
    }

    @Override
    public V replace(K key, V value, long lifespan, TimeUnit unit) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public boolean replace(K key, V oldValue, V value, long lifespan, TimeUnit unit) {
        return false;  // TODO: Customise this generated block
    }

    @Override
    public V put(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public V putIfAbsent(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        // TODO: Customise this generated block
    }

    @Override
    public V replace(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public boolean replace(K key, V oldValue, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        return false;  // TODO: Customise this generated block
    }

    @Override
    public V remove(Object key) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        // TODO: Customise this generated block
    }

    @Override
    public void clear() {
        // TODO: Customise this generated block
    }

    @Override
    public Set<K> keySet() {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public Collection<V> values() {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public int size() {
        return 0;  // TODO: Customise this generated block
    }

    @Override
    public boolean isEmpty() {
        return false;  // TODO: Customise this generated block
    }

    @Override
    public boolean containsKey(Object o) {
        return false;  // TODO: Customise this generated block
    }

    @Override
    public boolean containsValue(Object o) {
        return false;  // TODO: Customise this generated block
    }

    @Override
    public V get(Object k) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public V getLatest(K key, IncrementableEntryVersion upperBound) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public V getEarliest(K key, IncrementableEntryVersion lowerBound) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public IncrementableEntryVersion getLatestVersion(K key) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public IncrementableEntryVersion getLatestVersion(K key, IncrementableEntryVersion upperBound) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public IncrementableEntryVersion getEarliestVersion(K key) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public IncrementableEntryVersion getEarliestVersion(K key, IncrementableEntryVersion lowerBound) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public NotifyingFuture<V> putAsync(K key, V value) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public NotifyingFuture<V> putAsync(K key, V value, long lifespan, TimeUnit unit) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public NotifyingFuture<V> putAsync(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public NotifyingFuture<Void> putAllAsync(Map<? extends K, ? extends V> data) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public NotifyingFuture<Void> putAllAsync(Map<? extends K, ? extends V> data, long lifespan, TimeUnit unit) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public NotifyingFuture<Void> putAllAsync(Map<? extends K, ? extends V> data, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public NotifyingFuture<Void> clearAsync() {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public NotifyingFuture<V> putIfAbsentAsync(K key, V value) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public NotifyingFuture<V> putIfAbsentAsync(K key, V value, long lifespan, TimeUnit unit) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public NotifyingFuture<V> putIfAbsentAsync(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public NotifyingFuture<V> removeAsync(Object key) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public NotifyingFuture<Boolean> removeAsync(Object key, Object value) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public NotifyingFuture<V> replaceAsync(K key, V value) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public NotifyingFuture<V> replaceAsync(K key, V value, long lifespan, TimeUnit unit) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public NotifyingFuture<V> replaceAsync(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public NotifyingFuture<Boolean> replaceAsync(K key, V oldValue, V newValue) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public NotifyingFuture<Boolean> replaceAsync(K key, V oldValue, V newValue, long lifespan, TimeUnit unit) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public NotifyingFuture<Boolean> replaceAsync(K key, V oldValue, V newValue, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public NotifyingFuture<V> getAsync(K key) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public V putIfAbsent(K k, V v) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public boolean remove(Object o, Object o2) {
        return false;  // TODO: Customise this generated block
    }

    @Override
    public boolean replace(K k, V v, V v2) {
        return false;  // TODO: Customise this generated block
    }

    @Override
    public V replace(K k, V v) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public void start() {
        // TODO: Customise this generated block
    }

    @Override
    public void stop() {
        // TODO: Customise this generated block
    }

    private class HibernateProxy{

        K k;
        V v;
        IncrementableEntryVersion version;

        public HibernateProxy(K k, V v, IncrementableEntryVersion version){

        }

    }

}
