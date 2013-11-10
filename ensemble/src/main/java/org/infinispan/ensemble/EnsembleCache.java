 package org.infinispan.ensemble;

 import org.infinispan.AdvancedCache;
 import org.infinispan.Cache;
 import org.infinispan.configuration.cache.Configuration;
 import org.infinispan.lifecycle.ComponentStatus;
 import org.infinispan.manager.EmbeddedCacheManager;
 import org.infinispan.notifications.KeyFilter;
 import org.infinispan.util.concurrent.NotifyingFuture;

 import java.util.Collection;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.concurrent.ConcurrentMap;
 import java.util.concurrent.TimeUnit;

 /**
 *
 * TODO change the atomic object factory to support a basic cache.
 * The atomicity of the cache will be thus part of the property on the put operation of the underlying cache.
 * Hence, this will be possible to use an assembled cache as input of the factory.
 *
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public class EnsembleCache<K,V> implements Cache<K,V> {

    private String name;
    private Collection<ConcurrentMap<K,V>> caches;
    private ConcurrentMap<K,V> primary;

    public EnsembleCache(String name, List<ConcurrentMap<K, V>> caches){
        this.name = name;
        this.caches = caches;
        this.primary = caches.iterator().next();
    }

    public void addCache(Cache<K,V> cache){
         caches.add(cache);
    }

    public void removeCache(ConcurrentMap<K,V> cache){
        caches.remove(cache);
        if(cache == primary)
            primary = caches.iterator().next();
    }

    public String getName() {
        return name;
    }

     /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return primary.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return primary.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(Object k) {
        return primary.containsKey(k);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsValue(Object k) {
        return primary.containsValue(k);
    }

    /**
     * {@inheritDoc}
     *
     * Notice that if the replication factor is greater than 1, there is no consistency guarantee.
     * Otherwise, the consistency of the concerned cache applies.
     */
    @Override
    public V get(Object k) {
        return primary.get(k);

    }

    /**
     * {@inheritDoc}
     *
     * Notice that if the replication factor is greater than 1, there is no consistency guarantee.
     * Otherwise, the consistency of the concerned cache applies.
     */
    @Override
    public V put(K key, V value) {
        V ret = null;
        for(ConcurrentMap<K,V> c : caches)
            ret = c.put(key,value);
        return ret;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        for(ConcurrentMap<K,V> c: caches){
            c.putAll(map);
        }
    }

    @Override
    public void clear() {
        for(ConcurrentMap<K,V> c: caches){
            c.clear();
        }
    }

    @Override
    public Set<K> keySet() {
        return primary.keySet();
    }

    @Override
    public Collection<V> values() {
        return primary.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return primary.entrySet();
    }

    @Override
    public V putIfAbsent(K k, V v) {
        V ret = null;
        for(ConcurrentMap<K,V> c: caches){
            ret = (V) c.putIfAbsent(k, v);
        }
        return ret;
    }

    @Override
    public boolean remove(Object o, Object o2) {
        boolean ret = false;

        for(ConcurrentMap c: caches){
            ret |= c.remove(o,o2);
        }
        return ret;
    }

    @Override
    public boolean replace(K k, V v, V v2) {
        boolean ret = false;
        for(ConcurrentMap<K,V> c: caches){
            ret |= c.replace(k,v,v2);
        }
        return ret;
    }

    @Override
    public V replace(K k, V v) {
        V ret = null;
        for(ConcurrentMap<K,V> c: caches){
            ret = (V) c.replace(k,v);
        }
        return ret;
    }


     /*
     * NOT YET IMPLEMENTED INTERFACES.
     *
      */


     @Override
     public String getVersion() {
         return null;  // TODO: Customise this generated block
     }

     @Override
     public void putForExternalRead(K key, V value) {
         // TODO: Customise this generated block
     }

     @Override
     public void evict(K key) {
         // TODO: Customise this generated block
     }

     @Override
     public Configuration getCacheConfiguration() {
         return null;  // TODO: Customise this generated block
     }

     @Override
     public EmbeddedCacheManager getCacheManager() {
         return null;  // TODO: Customise this generated block
     }

     @Override
     public AdvancedCache<K, V> getAdvancedCache() {
         return null;  // TODO: Customise this generated block
     }

     @Override
     public ComponentStatus getStatus() {
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
     public V remove(Object o) {
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
     public boolean startBatch() {
         return false;  // TODO: Customise this generated block
     }

     @Override
     public void endBatch(boolean successful) {
         // TODO: Customise this generated block
     }

     @Override
     public void addListener(Object listener, KeyFilter filter) {
         // TODO: Customise this generated block
     }

     @Override
     public void start() {
         // TODO: Customise this generated block
     }

     @Override
     public void stop() {
         // TODO: Customise this generated block
     }

     @Override
     public void addListener(Object listener) {
         // TODO: Customise this generated block
     }

     @Override
     public void removeListener(Object listener) {
         // TODO: Customise this generated block
     }

     @Override
     public Set<Object> getListeners() {
         return null;  // TODO: Customise this generated block
     }
 }
