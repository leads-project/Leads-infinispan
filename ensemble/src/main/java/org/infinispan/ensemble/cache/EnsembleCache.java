package org.infinispan.ensemble.cache;

import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.util.concurrent.NotifyingFuture;
import org.infinispan.ensemble.EnsembleCacheManager;
import org.infinispan.ensemble.indexing.Indexable;
import org.infinispan.ensemble.indexing.Primary;
import org.infinispan.ensemble.indexing.Stored;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 *
 * An EnsembleCache offers a BasicCache API over a list of other EnsembleCaches.
 * Such an abstraction is of interest in various cases, e.g., when aggregating multiple Infinispan deployments.
 *
 * @author Pierre Sutra
 * @since 7.0
 */
public abstract class EnsembleCache<K,V> extends Indexable implements BasicCache<K,V> {

    //
    // CLASS FIELDS
    //

    protected static final Log log = LogFactory.getLog(EnsembleCache.class);

    //
    // OBJECT FIELDS
    //

    @Primary
    @Stored
    protected String name;

    @Stored
    protected List<? extends EnsembleCache<K,V>> caches;

    public EnsembleCache(String name, List<? extends EnsembleCache<K,V>> caches){
        this.name = name;
        this.caches= caches;
    }

    //
    // PUBLIC
    //

    @Override
    public String getVersion() {
        return Integer.toString(EnsembleCacheManager.ENSEMBLE_VERSION_MINOR)+"."+Integer.toString(EnsembleCacheManager.ENSEMBLE_VERSION_MAJOR);
    }

    public String getName() {
        return name;
    }

    @Override
    public void start() {
        for(BasicCache<K,V> c: caches)
            c.start();
    }

    @Override
    public void stop() {
        for(BasicCache<K,V> c: caches)
            c.stop();
    }

    //
    // NYI
    //

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }
    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsValue(Object k) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V putIfAbsent(K k, V v) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o, Object o2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean replace(K k, V v, V v2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V replace(K k, V v) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsKey(Object k) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V put(K key, V value, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V putIfAbsent(K key, V value, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V replace(K key, V value, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean replace(K key, V oldValue, V value, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V put(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V putIfAbsent(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V replace(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean replace(K key, V oldValue, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<V> putAsync(K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<V> putAsync(K key, V value, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<V> putAsync(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Void> putAllAsync(Map<? extends K, ? extends V> data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Void> putAllAsync(Map<? extends K, ? extends V> data, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Void> putAllAsync(Map<? extends K, ? extends V> data, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Void> clearAsync() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<V> putIfAbsentAsync(K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<V> putIfAbsentAsync(K key, V value, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<V> putIfAbsentAsync(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<V> removeAsync(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Boolean> removeAsync(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<V> replaceAsync(K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<V> replaceAsync(K key, V value, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<V> replaceAsync(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Boolean> replaceAsync(K key, V oldValue, V newValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Boolean> replaceAsync(K key, V oldValue, V newValue, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Boolean> replaceAsync(K key, V oldValue, V newValue, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<V> getAsync(K key) {
        throw new UnsupportedOperationException();
    }

}
