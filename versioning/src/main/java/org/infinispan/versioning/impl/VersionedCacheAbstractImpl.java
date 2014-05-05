package org.infinispan.versioning.impl;

import org.infinispan.Cache;
import org.infinispan.commons.util.concurrent.NotifyingFuture;
import org.infinispan.versioning.VersionedCache;
import org.infinispan.versioning.utils.version.Version;
import org.infinispan.versioning.utils.version.VersionGenerator;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 *
 * A basic implementation for a versioned cache.
 * This implementation is abstract.
 * A concrete implementation needs only to write two methods to be functional.
 * It is however advised to overload more methods to be efficient, as the algorithmic structure is quite basic.
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public abstract class VersionedCacheAbstractImpl<K,V> implements VersionedCache<K,V> {

    protected Cache delegate;
    protected VersionGenerator generator;
    protected String name;

    public VersionedCacheAbstractImpl(Cache<K, ?> delegate, VersionGenerator generator, String name) {
        this.delegate = delegate;
        this.generator = generator;
        this.name = name;
    }

    @Override
    public void put(K key, V value, Version version) {
        versionMapPut(key, value, version);
    }

    /**
     *
     * Return all the version between <i><first/i> and <i>last</i> exclusive.
     *
     * @param key
     * @param first
     * @param last
     * @return
     */
    @Override
    public Collection<Version> get(K key, Version first, Version last) {
        SortedMap<Version,V> map = versionMapGet(key);
        if(map.isEmpty())
            return Collections.EMPTY_LIST;
        return map.subMap(first, last).keySet();
    }

    @Override
    public V get(K key, Version version) {
        SortedMap<Version,V> map = versionMapGet(key);
        if(map.isEmpty())
            return null;
        return map.get(version);
    }

    @Override
    public V getLatest(K key, Version upperBound) {
        SortedMap<Version,V> map = versionMapGet(key);
        if(map.isEmpty())
            return null;
        return map.get(map.headMap(upperBound).lastKey());
    }

    @Override
    public V getEarliest(K key, Version lowerBound) {
        SortedMap<Version,V> map = versionMapGet(key);
        if(map.isEmpty())
            return null;
        return map.get(map.tailMap(lowerBound).firstKey());
    }

    @Override
    public Version getLatestVersion(K key) {
        SortedMap<Version,V> map = versionMapGet(key);
        if(map.isEmpty())
            return null;
        return map.lastKey();
    }

    @Override
    public Version getLatestVersion(K key, Version upperBound) {
        SortedMap<Version,V> map = versionMapGet(key);
        if(map.isEmpty())
            return null;
        return map.tailMap(upperBound).firstKey();
    }

    @Override
    public Version getEarliestVersion(K key) {
        SortedMap<Version,V> map = versionMapGet(key);
        if(map.isEmpty())
            return null;
        return map.firstKey();
    }

    @Override
    public Version getEarliestVersion(K key, Version lowerBound) {
        SortedMap<Version,V> map = versionMapGet(key);
        if(map.isEmpty())
            return null;
        return map.headMap(lowerBound).firstKey();
    }

    @Override
    public V get(Object o) {
        SortedMap<Version,V> map = versionMapGet((K) o);
        if(map.isEmpty()) return null;
        return map.get(map.lastKey());
    }

    @Override
    public Collection<V> values() {
        Collection<V> result = new ArrayList<V>();
        for(K key: keySet()){
            SortedMap<Version,V> map = versionMapGet(key);
            result.addAll(map.values());
        }
        return result;
    }

    @Override
    public V put(K key, V value) {
        Version lversion = getLatestVersion(key);
        Version nversion = null;
        V lval=null;
        if(lversion==null){
            nversion = generator.generateNew();
        }else{
            lval = get(key,lversion);
            nversion = generator.increment(lversion);
        }
        put(key,value,nversion);
        return lval;
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public void putAll(K k, Map<Version,V> map){
        for(Version v: map.keySet()){
            versionMapPut(k,map.get(v),v);
        }
    }


    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public String toString(){
        return delegate.toString();
    }


    //
    // ABSTRACT METHODS
    //

    protected abstract SortedMap<Version,V> versionMapGet(K key);

    protected abstract void versionMapPut(K key, V value, Version version);


    //
    // NYI METHODS
    //

    @Override
    public int size() {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public NotifyingFuture<V> putAsync(K key, V value) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public NotifyingFuture<V> putAsync(K key, V value, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public NotifyingFuture<V> putAsync(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public NotifyingFuture<Void> putAllAsync(Map<? extends K, ? extends V> data) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public NotifyingFuture<Void> putAllAsync(Map<? extends K, ? extends V> data, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public NotifyingFuture<Void> putAllAsync(Map<? extends K, ? extends V> data, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public NotifyingFuture<Void> clearAsync() {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public NotifyingFuture<V> putIfAbsentAsync(K key, V value) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public NotifyingFuture<V> putIfAbsentAsync(K key, V value, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public NotifyingFuture<V> putIfAbsentAsync(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public NotifyingFuture<V> removeAsync(Object key) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public NotifyingFuture<Boolean> removeAsync(Object key, Object value) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public NotifyingFuture<V> replaceAsync(K key, V value) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public NotifyingFuture<V> replaceAsync(K key, V value, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public NotifyingFuture<V> replaceAsync(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public NotifyingFuture<Boolean> replaceAsync(K key, V oldValue, V newValue) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public NotifyingFuture<Boolean> replaceAsync(K key, V oldValue, V newValue, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public NotifyingFuture<Boolean> replaceAsync(K key, V oldValue, V newValue, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public NotifyingFuture<V> getAsync(K key) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public String getVersion() {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public V put(K key, V value, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public V putIfAbsent(K key, V value, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map, long lifespan, TimeUnit unit) {
        // TODO: Customise this generated block
    }

    @Override
    public V replace(K key, V value, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public boolean replace(K key, V oldValue, V value, long lifespan, TimeUnit unit) {
        return false;  // TODO: Customise this generated block
    }

    @Override
    public V put(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public V putIfAbsent(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public V replace(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public boolean replace(K key, V oldValue, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public V putIfAbsent(K k, V v) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public boolean remove(Object o, Object o2) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public boolean replace(K k, V v, V v2) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public V replace(K k, V v) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public boolean containsValue(Object arg0) {
        throw new UnsupportedOperationException("NYI");
    }

}
