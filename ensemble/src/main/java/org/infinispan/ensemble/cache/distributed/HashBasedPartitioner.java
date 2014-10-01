package org.infinispan.ensemble.cache.distributed;

import org.infinispan.ensemble.cache.EnsembleCache;

import java.util.List;

/**
 *
 * @author Pierre Sutra
 * @since 7.0
 */
public class HashBasedPartitioner<K,V> extends Partitioner<K,V> {

    List<EnsembleCache<K,V>> cacheList;

    public HashBasedPartitioner(List<EnsembleCache<K,V>> cacheList){
        this.cacheList = cacheList;
    }

    @Override
    public EnsembleCache<K, V> locate(K k) {
        return cacheList.get(k.hashCode()%cacheList.size());
    }

}
