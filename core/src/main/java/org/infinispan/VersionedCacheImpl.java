package org.infinispan;

import org.infinispan.atomic.AtomicObjectFactory;
import org.infinispan.container.versioning.EntryVersionTreeMap;
import org.infinispan.container.versioning.IncrementableEntryVersion;
import org.infinispan.container.versioning.VersionGenerator;
import org.infinispan.util.concurrent.NotifyingFuture;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * // TODO: Document this
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public class VersionedCacheImpl<K,V> implements VersionedCache<K,V> {

    AtomicObjectFactory factory;
    Cache delegate;
    VersionGenerator generator;

    public VersionedCacheImpl(Cache delegate, VersionGenerator generator, String name) {
        this.delegate = delegate;
        this.generator = generator;
        factory = new AtomicObjectFactory((Cache<Object, Object>) delegate);
    }

    @Override
    public void put(K key, V value, IncrementableEntryVersion version) {
        TreeMap m = factory.getInstanceOf(EntryVersionTreeMap.class,key,true,null,false);
        m.put(version,value);
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
        SortedMap<IncrementableEntryVersion,V> map = factory.getInstanceOf(EntryVersionTreeMap.class,key,true,null,false);
        return map.subMap(first, last).values();
    }

    @Override
    public V get(K key, IncrementableEntryVersion version) {
        SortedMap<IncrementableEntryVersion,V> map = factory.getInstanceOf(EntryVersionTreeMap.class,key,true,null,false);
        return map.get(version);
    }

    @Override
    public V getLatest(K key, IncrementableEntryVersion upperBound) {
        SortedMap<IncrementableEntryVersion,V> map = factory.getInstanceOf(EntryVersionTreeMap.class,key,true,null,false);
        return map.get(map.headMap(upperBound).lastKey());
    }

    @Override
    public V getEarliest(K key, IncrementableEntryVersion lowerBound) {
        SortedMap<IncrementableEntryVersion,V> map = factory.getInstanceOf(EntryVersionTreeMap.class,key,true,null,false);
        return map.get(map.tailMap(lowerBound).firstKey());
    }

    @Override
    public IncrementableEntryVersion getLatestVersion(K key) {
        SortedMap<IncrementableEntryVersion,V> map = factory.getInstanceOf(EntryVersionTreeMap.class,key,true,null,false);
        return map.lastKey();
    }

    @Override
    public IncrementableEntryVersion getLatestVersion(K key, IncrementableEntryVersion upperBound) {
        SortedMap<IncrementableEntryVersion,V> map = factory.getInstanceOf(EntryVersionTreeMap.class,key,true,null,false);
        return map.tailMap(upperBound).firstKey();
    }

    @Override
    public IncrementableEntryVersion getEarliestVersion(K key) {
        SortedMap<IncrementableEntryVersion,V> map = factory.getInstanceOf(EntryVersionTreeMap.class,key,true,null,false);
        return map.firstKey();
    }

    @Override
    public IncrementableEntryVersion getEarliestVersion(K key, IncrementableEntryVersion lowerBound) {
        SortedMap<IncrementableEntryVersion,V> map = factory.getInstanceOf(EntryVersionTreeMap.class,key,true,null,false);
        return map.headMap(lowerBound).firstKey();
    }


    //
    // MAP INTERFACE
    //

    @Override
    public int size() {
        int result=0;
        for(Object key: delegate.keySet()){
            TreeMap m = factory.getInstanceOf(EntryVersionTreeMap.class,key,true,null,false);
            result += m.size();
        }
        return result;
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
        TreeMap m = factory.getInstanceOf(EntryVersionTreeMap.class,o,true,null,false);
        return m.containsValue(o);
    }

    @Override
    public V get(Object o) {
        TreeMap m = factory.getInstanceOf(EntryVersionTreeMap.class,o,true,null,false);
        return (V) m.get(m.lastKey());
    }

    @Override
    public Set<K> keySet() {
        return delegate.keySet();
    }

    @Override
    public Collection<V> values() {
        Collection<V> result = new ArrayList<V>();
        for(Object key: delegate.keySet()){
            TreeMap m = factory.getInstanceOf(EntryVersionTreeMap.class,key,true,null,false);
            result.addAll(m.values());
        }
        return result;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K,V>> result = new HashSet<Entry<K, V>>();
        for(Object key: delegate.keySet()){
            TreeMap m = factory.getInstanceOf(EntryVersionTreeMap.class,key,true,null,false);
            result.addAll(m.entrySet());
        }
        return result;
    }

    @Override
    public V put(K key, V value) {
        SortedMap<IncrementableEntryVersion,V> map = factory.getInstanceOf(EntryVersionTreeMap.class,key,true,null,false);
        IncrementableEntryVersion lversion;
        V lval;

        if(map.isEmpty()){
            lversion = generator.generateNew();
            lval = null;
        }else{
            lversion =  map.lastKey();
            lval = map.get(lversion);
        }
        IncrementableEntryVersion entryVersion = generator.increment(lversion);
        put(key,value,entryVersion);
        return lval;
    }

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
    public void start() {
        // TODO: Customise this generated block
    }

    @Override
    public void stop() {
        // TODO: Customise this generated block
    }

}
