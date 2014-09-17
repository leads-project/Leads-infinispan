package org.infinispan.ensemble.cache.distributed;

import org.infinispan.ensemble.cache.EnsembleCache;

/**
 *
 * @author Pierre Sutra
 * @since 7.0
 */
public abstract class Partitioner<K,V>{

    public abstract EnsembleCache<K,V> locate(K k);

}
