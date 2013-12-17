package org.infinispan.ensemble;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.VersionedValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public class SWMREnsembleCache<K,V> extends EnsembleCache<K,V> {

    protected List<RemoteCache<K,V>> caches;

    public SWMREnsembleCache(String name, List<RemoteCache<K,V>> caches){
        super(name,caches);
    }


    @Override
    public V get(Object key) {
        return latestValue((K)key).getValue();
    }

    @Override
    public V put(K key, V value) {
        VersionedValue<V> v = latestValue(key);
        if(v==null){
            for(RemoteCache<K,V> c :quorumCache()){
                c.put(key,value);
            }
            return null;
        }

        for(RemoteCache<K,V> c :quorumCache()){
            c.replaceWithVersion(key,value,v.getVersion());
        }
        return v.getValue();
    }

    //
    // OBJECT METHODS
    //

    protected List<RemoteCache<K,V>> quorumCache(){
        Collections.shuffle(caches);
        List<RemoteCache<K,V>> q = new ArrayList<RemoteCache<K,V>>();
        for(int i=0; i<q.size()/2+1; i++)
            q.add(caches.get(i));
        return q;
    }

    protected VersionedValue<V> latestValue(K k){
        VersionedValue<V> ret = null;
        for(RemoteCache<K,V> cache : quorumCache()){
            VersionedValue<V> tmp = cache.getVersioned(k);
            if(tmp !=  null && tmp.getVersion()>ret.getVersion())
                ret = tmp;
        }
        return ret;
    }
}
