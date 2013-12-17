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

}
