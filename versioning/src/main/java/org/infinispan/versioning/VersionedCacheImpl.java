package org.infinispan.versioning;

import org.infinispan.Cache;
import org.infinispan.commons.util.concurrent.NotifyingFuture;
import org.infinispan.container.versioning.IncrementableEntryVersion;
import org.infinispan.container.versioning.VersionGenerator;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * // TODO: Document this
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public abstract class VersionedCacheImpl <K,V> implements VersionedCache<K,V> {

    protected Cache<K,?> delegate;
    protected VersionGenerator generator;
    protected String name;

    public VersionedCacheImpl(Cache<K,?> delegate, VersionGenerator generator, String name) {
        this.delegate = delegate;
        this.generator = generator;
        this.name = name;
    }

    @Override
    public void put(K key, V value, IncrementableEntryVersion version) {
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
    public Collection<V> get(K key, IncrementableEntryVersion first, IncrementableEntryVersion last) {
        SortedMap<IncrementableEntryVersion,V> map = versionMapGet(key);
        if(map.isEmpty())
            return null;
        return map.subMap(first, last).values();
    }

    @Override
    public V get(K key, IncrementableEntryVersion version) {
        SortedMap<IncrementableEntryVersion,V> map = versionMapGet(key);
        if(map.isEmpty())
            return null;
        return map.get(version);
    }

    @Override
    public V getLatest(K key, IncrementableEntryVersion upperBound) {
        SortedMap<IncrementableEntryVersion,V> map = versionMapGet(key);
        if(map.isEmpty())
            return null;
        return map.get(map.headMap(upperBound).lastKey());
    }

    @Override
    public V getEarliest(K key, IncrementableEntryVersion lowerBound) {
        SortedMap<IncrementableEntryVersion,V> map = versionMapGet(key);
        if(map.isEmpty())
            return null;
        return map.get(map.tailMap(lowerBound).firstKey());
    }

    @Override
    public IncrementableEntryVersion getLatestVersion(K key) {
        SortedMap<IncrementableEntryVersion,V> map = versionMapGet(key);
        if(map.isEmpty())
            return null;
        return map.lastKey();
    }

    @Override
    public IncrementableEntryVersion getLatestVersion(K key, IncrementableEntryVersion upperBound) {
        SortedMap<IncrementableEntryVersion,V> map = versionMapGet(key);
        if(map.isEmpty())
            return null;
        return map.tailMap(upperBound).firstKey();
    }

    @Override
    public IncrementableEntryVersion getEarliestVersion(K key) {
        SortedMap<IncrementableEntryVersion,V> map = versionMapGet(key);
        if(map.isEmpty())
            return null;
        return map.firstKey();
    }

    @Override
    public IncrementableEntryVersion getEarliestVersion(K key, IncrementableEntryVersion lowerBound) {
        SortedMap<IncrementableEntryVersion,V> map = versionMapGet(key);
        if(map.isEmpty())
            return null;
        return map.headMap(lowerBound).firstKey();
    }

    @Override
    public int size() {
        int result=0;
        for(K key: keySet()){
            SortedMap<IncrementableEntryVersion,V> map = versionMapGet(key);
            result += map.size();
        }
        return result;
    }

    @Override
    public V get(Object o) {
        return get((K) o, getLatestVersion((K) o));
    }

    @Override
    public Collection<V> values() {
        Collection<V> result = new ArrayList<V>();
        for(K key: keySet()){
            SortedMap<IncrementableEntryVersion,V> map = versionMapGet(key);
            result.addAll(map.values());
        }
        return result;
    }

    @Override
    public V put(K key, V value) {
        IncrementableEntryVersion lversion = getLatestVersion(key);
        IncrementableEntryVersion nversion = null;
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

    //
    // OBJECT METHODS
    //

    protected abstract SortedMap<IncrementableEntryVersion,V> versionMapGet(K key);

    protected abstract void versionMapPut(K key, V value, IncrementableEntryVersion verrsion);


    //
    // NYI
    //

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
    public String getName() {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public String getVersion() {
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
    public Set<Entry<K, V>> entrySet() {
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
}
