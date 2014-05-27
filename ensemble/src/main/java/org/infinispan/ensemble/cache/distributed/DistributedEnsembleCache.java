package org.infinispan.ensemble.cache.distributed;

import org.infinispan.ensemble.cache.EnsembleCache;

import java.util.List;

/**
 * @author Pierre Sutra
 * @since 7.0
 */
public class DistributedEnsembleCache<K,V> extends EnsembleCache<K,V> {

    private Partitioner<K,V> partitioner;

    public DistributedEnsembleCache(String name, List<? extends EnsembleCache<K, V>> caches, Partitioner<K, V> partitioner){
        super(name,caches);
        this.partitioner = partitioner;
    }

    @Override
    public int size() {
        int ret=0;
        for (EnsembleCache ensembleCache : caches){
            ret+=ensembleCache.size();
        }
        return ret;
    }

    @Override
    public boolean isEmpty() {
        for (EnsembleCache ensembleCache : caches)
            if (!ensembleCache.isEmpty())
                return false;
        return true;
    }

    @Override
    public V get(Object key) {
        return partitioner.locate((K)key).get(key);
    }

    @Override
    public V put(K key, V value) {
        return partitioner.locate(key).put(key,value);
    }

}
