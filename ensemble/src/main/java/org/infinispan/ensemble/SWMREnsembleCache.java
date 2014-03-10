package org.infinispan.ensemble;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.VersionedValue;
import org.infinispan.commons.util.concurrent.NotifyingFuture;

import java.util.*;
import java.util.concurrent.ExecutionException;


/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public class SWMREnsembleCache<K,V> extends EnsembleCache<K,V> {

    public SWMREnsembleCache(String name, List<RemoteCache<K, V>> caches){
        super(name,caches);
    }

    @Override
    public V get(Object key) {
        Map<RemoteCache<K,V>, VersionedValue<V>> previous= previousValues((K)key);
        VersionedValue<V> g = greatestValue(previous);
        if(g==null)
            return null;
        if(!isStable(previous, g))
            writeStable((K) key, g.getValue(), previous.keySet());
        return g.getValue();
    }

    @Override
    public V put(K key, V value) {
        Map<RemoteCache<K,V>, VersionedValue<V>> previous= previousValues((K)key);
        VersionedValue<V> g = greatestValue(previous);
        writeStable(key, value, previous.keySet());
        if(g!=null)
            return g.getValue();
        return null;
    }

    //
    // OBJECT METHODS
    //

    private void writeStable(K key, V value, Set<RemoteCache<K, V>> caches) {
        List<NotifyingFuture<V>> futures = new ArrayList<NotifyingFuture<V>>();
        for(RemoteCache<K,V> c : caches) {
            futures.add(c.putAsync(key,value));
        }
        for(NotifyingFuture<V> future : futures){
            try {
                future.get();
            } catch (InterruptedException e) {
                e.printStackTrace();  // TODO: Customise this generated block
            } catch (ExecutionException e) {
                e.printStackTrace();  // TODO: Customise this generated block
            }
        }

    }

    private Map<RemoteCache<K,V>, VersionedValue<V>> previousValues(K k){
        Map<RemoteCache<K,V>,NotifyingFuture<VersionedValue<V>>> futures
                = new HashMap<RemoteCache<K, V>, NotifyingFuture<VersionedValue<V>>>();
        for(RemoteCache<K,V> cache : quorumCache()){
            futures.put(cache, cache.getVersionedAsynch(k));
        }
        Map<RemoteCache<K,V>, VersionedValue<V>> ret = new HashMap<RemoteCache<K, V>, VersionedValue<V>>();
        for(RemoteCache<K,V> cache : futures.keySet()){
            VersionedValue<V> tmp = null;
            try {
                tmp = futures.get(cache).get();
            } catch (InterruptedException e) {
                e.printStackTrace();  // TODO: Customise this generated block
            } catch (ExecutionException e) {
                e.printStackTrace();  // TODO: Customise this generated block
            }
            ret.put(cache,tmp);
        }
        return ret;
    }

    private boolean isStable(Map<RemoteCache<K, V>, VersionedValue<V>> map, VersionedValue<V> v){
        int count = 0;
        if(v==null) return true;
        for(VersionedValue<V> w: map.values()){
            if( w!=null && w.getVersion()==v.getVersion())
                count++;
        }
        return count >= quorumSize();
    }

    private VersionedValue<V> greatestValue(Map<RemoteCache<K,V>,VersionedValue<V>> map){
        VersionedValue<V> ret = null;
        for(VersionedValue<V> v: map.values()){
            if ( v!=null && (ret==null || v.getVersion()>ret.getVersion()))
                ret = v;
        }
        return ret;
    }

}
