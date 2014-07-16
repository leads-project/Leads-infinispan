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
 * @author Pierre Sutra
 * @since 6.0
 */
public abstract class VersionedCacheSplitAbstractImpl<K,V> implements VersionedCache<K,V> {

    protected Cache delegate;
    protected VersionGenerator generator;
    protected String name;

    public VersionedCacheSplitAbstractImpl(Cache delegate, VersionGenerator generator, String name) {
        this.delegate = delegate;
        this.generator = generator;
        this.name = name;
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
        SortedMap<Version, String> map = versionMapGet(key);
        if(map.isEmpty())
            return Collections.EMPTY_LIST;
        return map.subMap(first, last).keySet();
    }

    @Override
    public V get(K key, Version version) {
        SortedMap<Version, String> map = versionMapGet(key);
        if(!map.containsKey(version))
            return null;
        return retrieveVersionedValue(key,version);
    }

    @Override
    public V getLatest(K key, Version upperBound) {
        SortedMap<Version, String> map = versionMapGet(key);
        if(map.isEmpty())
            return null;
        Version version = map.headMap(upperBound).lastKey();
        return retrieveVersionedValue(key,version);
    }

    @Override
    public V getEarliest(K key, Version lowerBound) {
        SortedMap<Version, String> map = versionMapGet(key);
        if(map.isEmpty())
            return null;
        Version version = map.tailMap(lowerBound).firstKey();
        return retrieveVersionedValue(key,version);
    }

    @Override
    public Version getLatestVersion(K key) {
        SortedMap<Version, String> map = versionMapGet(key);
        if(map.isEmpty())
            return null;
        return map.lastKey();
    }

    @Override
    public Version getLatestVersion(K key, Version upperBound) {
        SortedMap<Version, String> map = versionMapGet(key);
        if(map.isEmpty())
            return null;
        return map.tailMap(upperBound).firstKey();
    }

    @Override
    public Version getEarliestVersion(K key) {
        SortedMap<Version, String> map = versionMapGet(key);
        if(map.isEmpty())
            return null;
        return map.firstKey();
    }

    @Override
    public Version getEarliestVersion(K key, Version lowerBound) {
        SortedMap<Version, String> map = versionMapGet(key);
        if(map.isEmpty())
            return null;
        return map.headMap(lowerBound).firstKey();
    }

    @Override
    public V get(Object o) {
        SortedMap<Version, String> map = versionMapGet((K) o);
        if(map.isEmpty())
            return null;
        Version version = map.lastKey();
        return retrieveVersionedValue((K)o,version);
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
    public void put(K key, V value, Version version) {
        String versionedValueKey = versionedValueKey(key,version);
        delegate.put(versionedValueKey,value);
        versionMapPut(key, versionedValueKey, version);
    }

    @Override
    public void putAll(K k, Map<Version,V> map){
        Map<Version, String> versionsAdded = new HashMap<>(map.size());
        Map<String, V> valuesAdded = new HashMap<>(map.size());
        for(Version v: map.keySet()){
            String versionedValueKey = versionedValueKey(k,v);
            versionsAdded.put(v, versionedValueKey);
            valuesAdded.put(versionedValueKey, map.get(v));
        }

        for(Map.Entry e : valuesAdded.entrySet()){
            delegate.put(e.getKey(),e.getValue());
        }

        versionMapPutAll(k, versionsAdded);
    }

    @Override
    public boolean containsKey(Object o) {
        return delegate.containsKey(o);
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public void clear() {
        delegate.clear();
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

    protected abstract SortedMap<Version,String> versionMapGet(K key);

    protected abstract void versionMapPut(K key, String value, Version version);

    protected abstract void versionMapPutAll(K k, Map<Version,String> map);



    // HELPER

    private String versionedValueKey(K key, Version version){
        return key.toString()+":"+version.toString();
    }

    private V retrieveVersionedValue(K key, Version version){
        return (V) delegate.get(versionedValueKey(key,version));
    }

    //
    // NYI METHODS
    //


    public Collection<V> values() {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException("NYI");
    }

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
