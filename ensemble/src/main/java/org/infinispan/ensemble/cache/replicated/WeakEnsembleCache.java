package org.infinispan.ensemble.cache.replicated;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.util.concurrent.NotifyingFuture;
import org.infinispan.ensemble.cache.EnsembleCache;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public class WeakEnsembleCache<K,V> extends ReplicatedEnsembleCache<K,V> {

    public WeakEnsembleCache(String name, List<EnsembleCache<K,V>> caches){
        super(name,caches);
    }

    @Override
    public V put(K key, V value) {
        V ret;
        List<NotifyingFuture<V>> futures = new ArrayList<NotifyingFuture<V>>();
        for(EnsembleCache<K,V> c : caches){
            futures.add(((RemoteCache)c).putAsync(key, value));
        }
        for(NotifyingFuture<V> f : futures){
            try {
                ret = f.get();
                return ret;
            } catch (InterruptedException e) {
                e.printStackTrace();  // TODO: Customise this generated block
            } catch (ExecutionException e) {
                e.printStackTrace();  // TODO: Customise this generated block
            }
        }
        return null;
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

    @Override
    public int size() {
        return someCache().size();
    }

    @Override
    public boolean isEmpty() {
        return someCache().isEmpty();
    }



}
