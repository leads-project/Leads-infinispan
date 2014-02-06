package org.infinispan.ensemble;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.util.concurrent.NotifyingFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public class WeakEnsembleCache<K,V> extends EnsembleCache<K,V> {

    public WeakEnsembleCache(String name, List<RemoteCache<K, V>> caches){
        super(name,caches);
    }

    @Override
    public V put(K key, V value) {
        V ret = null;
        List<NotifyingFuture<V>> futures = new ArrayList<NotifyingFuture<V>>();
        for(RemoteCache<K,V> c : caches){
            futures.add(c.putAsync(key, value));
        }
        for(NotifyingFuture<V> f : futures){
            try {
                ret = f.get();
            } catch (InterruptedException e) {
                e.printStackTrace();  // TODO: Customise this generated block
            } catch (ExecutionException e) {
                e.printStackTrace();  // TODO: Customise this generated block
            }
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


}
