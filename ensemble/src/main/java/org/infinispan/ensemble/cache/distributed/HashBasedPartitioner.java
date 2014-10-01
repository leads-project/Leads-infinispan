package org.infinispan.ensemble.cache.distributed;

import org.infinispan.ensemble.cache.EnsembleCache;

import java.util.List;

/**
 *
 * @author Pierre Sutra
 * @since 7.0
 */
public class HashBasedPartitioner<K,V> extends Partitioner<K,V> {

    public HashBasedPartitioner(List<EnsembleCache<K,V>> caches){
        super(caches);
    }

    @Override
    public EnsembleCache<K, V> locate(K k) {
        return caches.get(Math.abs(k.hashCode())%caches.size());
    }

}
