 package org.infinispan.ensemble;

 import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.api.BasicCache;
 import org.infinispan.commons.util.concurrent.NotifyingFuture;

 import java.util.*;
import java.util.concurrent.TimeUnit;

 /**
 *
 * The atomicity of the cache will be thus part of the property on the put operation of the underlying cache.
 * Hence, this will be possible to use an assembled cache as input of the factory.
 *
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public abstract class EnsembleCache<K,V> implements BasicCache<K,V> {

    protected String name;
    protected List<RemoteCache<K,V>> caches;

    public EnsembleCache(String name, List<RemoteCache<K, V>> caches){
        this.name = name;
        this.caches = caches;
    }


     //
     // PUBLIC METHODS
     //

     @Override
     public String getVersion() {
         return Integer.toString(EnsembleCacheManager.ENSEMBLE_VERSION_MINOR)+"."+Integer.toString(EnsembleCacheManager.ENSEMBLE_VERSION_MAJOR);
     }

     @Override
     public int size() {
         return someCache().size();
     }

     @Override
     public boolean isEmpty() {
         return someCache().isEmpty();
     }

     @Override
     public V put(K key, V value) {
         V ret = null;
         for(BasicCache<K,V> c : caches)
             ret = c.put(key,value);
         return ret;
     }

     @Override
     public V put(K key, V value, long lifespan, TimeUnit unit) {
         V ret = null;
         for(BasicCache<K,V> c : caches)
             ret = c.put(key,value,lifespan,unit);
         return ret;
     }

     @Override
     public V putIfAbsent(K key, V value, long lifespan, TimeUnit unit) {
         V ret = null;
         for(BasicCache<K,V> c : caches)
             ret = c.putIfAbsent(key, value, lifespan, unit);
         return ret;

     }

     @Override
     public void putAll(Map<? extends K, ? extends V> map, long lifespan, TimeUnit unit) {
         V ret = null;
         for(BasicCache<K,V> c : caches)
             c.putAll(map,lifespan,unit);
     }

     @Override
     public V replace(K key, V value, long lifespan, TimeUnit unit) {
         V ret = null;
         for(BasicCache<K,V> c : caches)
             ret = c.replace(key, value, lifespan, unit);
         return ret;

     }

     @Override
     public boolean replace(K key, V oldValue, V value, long lifespan, TimeUnit unit) {
         boolean  ret = false;
         for(BasicCache<K,V> c : caches)
             ret = c.replace(key, oldValue, value, lifespan, unit);
         return ret;

     }

     @Override
     public V put(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
         V ret = null;
         for(BasicCache<K,V> c : caches)
             ret = c.put(key,value,lifespan,lifespanUnit,maxIdleTime,maxIdleTimeUnit);
         return ret;
     }

     @Override
     public V putIfAbsent(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
         V ret = null;
         for(BasicCache<K,V> c : caches)
             ret = c.putIfAbsent(key,value,lifespan,lifespanUnit,maxIdleTime,maxIdleTimeUnit);
         return ret;
     }

     @Override
     public void putAll(Map<? extends K, ? extends V> map, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
         for(BasicCache<K,V> c : caches)
             c.putAll(map,lifespan,lifespanUnit,maxIdleTime,maxIdleTimeUnit);
     }

     @Override
     public V replace(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
         V ret = null;
         for(BasicCache<K,V> c : caches)
             ret = c.replace(key,value,lifespan,lifespanUnit,maxIdleTime,maxIdleTimeUnit);
         return ret;
     }

     @Override
     public boolean replace(K key, V oldValue, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
         boolean ret = false;
         for(BasicCache<K,V> c : caches)
             ret = c.replace(key,oldValue, value,lifespan,lifespanUnit,maxIdleTime,maxIdleTimeUnit);
         return ret;
     }

     @Override
     public V remove(Object key) {
         V ret = null;
         for(BasicCache<K,V> c : caches)
             ret = c.remove(key);
         return ret;
     }

     @Override
     public void putAll(Map<? extends K, ? extends V> map) {
         V ret = null;
         for(BasicCache<K,V> c : caches)
             c.putAll(map);
     }

     @Override
     public void clear() {
         for(BasicCache<K,V> c: caches){
             c.clear();
         }
     }

     @Override
     public Set<K> keySet() {
         return firstCache().keySet();
     }
     @Override
     public Collection<V> values() {
         return firstCache().values();
     }

     @Override
     public Set<Entry<K, V>> entrySet() {
         return firstCache().entrySet();
     }

     @Override
     public V putIfAbsent(K k, V v) {
         V ret = null;
         for(BasicCache<K,V> c: caches){
             ret = (V) c.putIfAbsent(k, v);
         }
         return ret;
     }

     @Override
     public boolean remove(Object o, Object o2) {
         boolean ret = false;

         for(BasicCache c: caches){
             ret |= c.remove(o,o2);
         }
         return ret;
     }

     @Override
     public boolean replace(K k, V v, V v2) {
         boolean ret = false;
         for(BasicCache<K,V> c: caches){
             ret |= c.replace(k,v,v2);
         }
         return ret;
     }

     @Override
     public V replace(K k, V v) {
         V ret = null;
         for(BasicCache<K,V> c: caches){
             ret = (V) c.replace(k,v);
         }
         return ret;
     }



     /**
      * {@inheritDoc}
      *
      * Notice that if the replication factor is greater than 1, there is no consistency guarantee.
      * Otherwise, the consistency of the concerned cache applies.
      */
     @Override
     public V get(Object k) {
         return someCache().get(k);
     }

     /**
      * {@inheritDoc}
      */
     @Override
     public boolean containsKey(Object k) {
         return someCache().containsKey(k);
     }

     /**
      * {@inheritDoc}
      */
     @Override
     public boolean containsValue(Object k) {
         return someCache().containsValue(k);
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
     // OBJECT METHODS
     //

     protected BasicCache<K,V> firstCache(){
         return caches.iterator().next();
     }

     protected BasicCache<K,V> someCache(){
         Collections.shuffle(caches);
         return caches.iterator().next();
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



 }
