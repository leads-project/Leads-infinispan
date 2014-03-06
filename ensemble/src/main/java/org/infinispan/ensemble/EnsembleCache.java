 package org.infinispan.ensemble;

 import org.infinispan.client.hotrod.RemoteCache;
 import org.infinispan.commons.api.BasicCache;
 import org.infinispan.commons.util.concurrent.NotifyingFuture;

 import java.util.*;
 import java.util.concurrent.TimeUnit;

 /**
 *
 * The atomicity of the cache will be thus part of the property on the writeBack operation of the underlying cache.
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

     public String getName() {
         return name;
     }

     @Override
     public void start() {
         for(RemoteCache<K,V> c: caches)
             c.start();
     }

     @Override
     public void stop() {
         for(RemoteCache<K,V> c: caches)
             c.stop();
     }

     //
     // OBJECT METHODS
     //

     protected RemoteCache<K,V> firstCache(){
         return caches.iterator().next();
     }

     protected RemoteCache<K,V> someCache(){
         Collections.shuffle(caches);
         return caches.iterator().next();
     }

     protected int quorumSize(){
         return (int)Math.floor((double)caches.size()/(double)2) +1;
     }

     protected Collection<RemoteCache<K,V>> quorumCache(){
         List<RemoteCache<K,V>> quorum = new ArrayList<RemoteCache<K, V>>();
         for(int i=0; i< quorumSize(); i++){
             quorum.add(caches.get(i));
         }
         assert quorum.size() == quorumSize();
         return quorum;
     }

     protected Collection<RemoteCache<K,V>> quorumCacheContaining(RemoteCache<K, V> cache){
         List<RemoteCache<K,V>> quorum = new ArrayList<RemoteCache<K, V>>();
         quorum.add(cache);
         for(int i=0; quorum.size()<quorumSize(); i++){
             if(!caches.get(i).equals(cache))
                 quorum.add(caches.get(i));
         }
         assert quorum.size() == quorumSize();
         return quorum;
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
         return someCache().containsKey(k);
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
