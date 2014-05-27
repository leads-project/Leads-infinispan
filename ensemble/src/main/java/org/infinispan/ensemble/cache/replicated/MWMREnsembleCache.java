package org.infinispan.ensemble.cache.replicated;

import org.infinispan.commons.api.BasicCache;
import org.infinispan.ensemble.cache.EnsembleCache;

import java.util.List;

/**
 * @author Pierre Sutra
 * @since 6.0
 */
public class MWMREnsembleCache<K,V> extends ReplicatedEnsembleCache<K,V> {

    BasicCache<K,V> primary;

    public MWMREnsembleCache(String name, List<EnsembleCache<K,V>> caches) {
        super(name, caches);
        primary = caches.iterator().next();
    }


    @Override
    public V get(Object o) {
        return primary.get(o);
    }

    @Override
    public V put(K key, V value) {
        V ret = primary.put(key, value);
        for(BasicCache<K,V> cache : quorumCacheContaining(primary)){
            if(!cache.equals(primary))
                cache.put(key,value);
        }
        return ret;
    }

}
