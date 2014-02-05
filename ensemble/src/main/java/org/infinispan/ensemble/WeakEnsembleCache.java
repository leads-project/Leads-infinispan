package org.infinispan.ensemble;

import org.infinispan.client.hotrod.RemoteCache;
import java.util.List;

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
        for(RemoteCache<K,V> c : quorumCache()){
            ret = c.put(key,value);
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
