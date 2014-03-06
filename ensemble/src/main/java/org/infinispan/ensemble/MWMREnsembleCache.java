package org.infinispan.ensemble;

import org.infinispan.client.hotrod.RemoteCache;

import java.util.List;

/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public class MWMREnsembleCache<K,V> extends EnsembleCache<K,V> { {
}

    RemoteCache<K,V> primary;

    public MWMREnsembleCache(String name, List<RemoteCache<K, V>> remoteCaches) {
        super(name, remoteCaches);
        primary = remoteCaches.iterator().next();
    }


    @Override
    public V get(Object o) {
        return primary.get(o);
    }

    @Override
    public V put(K key, V value) {
        V ret = primary.put(key, value);
        for(RemoteCache<K,V> cache : quorumCacheContaining(primary)){
            if(!cache.equals(primary))
                cache.put(key,value);
        }
        return ret;
    }

}
