 package org.infinispan.ensemble;

 import org.infinispan.client.hotrod.RemoteCache;
 import org.infinispan.commons.api.BasicCache;
 import org.infinispan.commons.util.concurrent.NotifyingFuture;

 import javax.xml.bind.annotation.XmlElement;
 import javax.xml.bind.annotation.XmlRootElement;
 import java.util.*;
 import java.util.concurrent.TimeUnit;

 /**
 *
 * An ensemble cache aggregates multiple BasicCaches.
 * This class is abstract and defines cores operations on the aggregated caches, such as retrieving a quorum of them.
 * The actual implementations of this abstract class are
 * @see org.infinispan.ensemble.MWMREnsembleCache,
 * @see org.infinispan.ensemble.SWMREnsembleCache, and
 * @see org.infinispan.ensemble.WeakEnsembleCache .
 *
 * @author Pierre Sutra
 * @since 6.0
 */

 @XmlRootElement(name="ensemble")
 public abstract class EnsembleCache<K,V> implements BasicCache<K,V> {

    protected  String name;
    protected List<Site> sites;
    protected List<RemoteCache<K,V>> caches;

    public EnsembleCache(String name, List<Site> sites){
        this.name = name;
        this.sites = sites;
        List<RemoteCache<K,V>> caches = new ArrayList<RemoteCache<K, V>>();
        for(Site site: sites){
            RemoteCache<K,V> c = site.getManager().getCache(name);
            assert site != null;
            assert site.getManager() != null : site.getName();
            caches.add(c);
        }
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

     @XmlElement(name="name")
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

     @XmlElement(name="sites")
     public List<Site> sites(){
         return sites;
     }

     //
     // OBJECT METHODS
     //

     protected RemoteCache<K,V> firstCache(){
         return caches.iterator().next();
     }

     protected RemoteCache<K,V> someCache(){
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
