package org.infinispan.ensemble.cache.replicated;

import org.infinispan.commons.api.BasicCache;
import org.infinispan.ensemble.cache.EnsembleCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * Existing implementations of this abstract class are:
 * @see MWMREnsembleCache
 * @see SWMREnsembleCache
 * @see WeakEnsembleCache
 *
 * @author Pierre Sutra
 * @since 7.0
 */
public abstract class ReplicatedEnsembleCache<K,V> extends EnsembleCache<K,V> {

    public ReplicatedEnsembleCache(String name, List<? extends EnsembleCache<K, V>> caches) {
        super(name, caches);
    }

    @Override
    public int size() {
        return someCache().size();
    }

    @Override
    public boolean isEmpty() {
        return someCache().isEmpty();
    }

    //
    // HELPERS
    //

    protected BasicCache<K,V> someCache(){
        return caches.iterator().next();
    }

    protected int quorumSize(){
        return (int)Math.floor((double)caches.size()/(double)2) +1;
    }

    protected Collection<BasicCache<K,V>> quorumCache(){
        List<BasicCache<K,V>> quorum = new ArrayList<BasicCache<K, V>>();
        for(int i=0; i< quorumSize(); i++){
            quorum.add(caches.get(i));
        }
        assert quorum.size() == quorumSize();
        return quorum;
    }

    protected Collection<BasicCache<K,V>> quorumCacheContaining(BasicCache<K, V> cache){
        List<BasicCache<K,V>> quorum = new ArrayList<BasicCache<K, V>>();
        quorum.add(cache);
        for(int i=0; quorum.size()<quorumSize(); i++){
            if(!caches.get(i).equals(cache))
                quorum.add(caches.get(i));
        }
        assert quorum.size() == quorumSize();
        return quorum;
    }

}
