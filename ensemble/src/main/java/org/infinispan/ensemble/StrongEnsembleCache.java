package org.infinispan.ensemble;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.VersionedValue;
import org.infinispan.commons.util.concurrent.NotifyingFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;


/**
 *
 * TODO this is a SWMR register.
 * To implement WMWMR, there are a few solutions:
 * (1) vector clocks, with one entry per writer.
 * (2) in case the participants are unknown, with the coniditional write API of a remoteCache,
 *     one can implement an obstruction-free timestamp, and on top of it, we can do a MWMR register;
 * (3) best approach: a version is now two integers, a TS and an ID; we use ID to break equalities.
 *
 * TODO we assume here that null cannot be written inside a cache.
 *
 * TODO double-check the write-back mechanism according to the total order semantics
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public class StrongEnsembleCache<K,V> extends EnsembleCache<K,V> {

    public StrongEnsembleCache(String name, List<RemoteCache<K, V>> caches){
        super(name,caches);
    }

    @Override
    public V get(Object key) {
        VersionedValue<V> v = latestValue((K)key);
        if(v!=null)
            writeBack((K)key,v.getValue(),v.getVersion());
        return v.getValue();
    }

    @Override
    public V put(K key, V value) {
        VersionedValue<V> v = latestValue(key);
        if(v==null){
            for(RemoteCache<K,V> c :quorumCache()){
                c.putIfAbsent(key,value);
            }
            return null;
        }
        writeBack(key,value,v.getVersion());
        return v.getValue();
    }

    //
    // OBJECT METHODS
    //

    protected void writeBack(K key, V v, long version){
        List<NotifyingFuture<Boolean>> futures = new ArrayList<NotifyingFuture<Boolean>>();
        for(RemoteCache<K,V> c :quorumCache()){
            VersionedValue<V> tmp = c.getVersioned(key);
            if(tmp.getVersion() < version)
                futures.add(c.replaceWithVersionAsync(key, v, tmp.getVersion()));
        }
        for(NotifyingFuture<Boolean> future : futures){
            try {
                future.get();
            } catch (InterruptedException e) {
                e.printStackTrace();  // TODO: Customise this generated block
            } catch (ExecutionException e) {
                e.printStackTrace();  // TODO: Customise this generated block
            }
        }
    }

    protected VersionedValue<V> latestValue(K k){
        List<NotifyingFuture<VersionedValue<V>>> futures = new ArrayList<NotifyingFuture<VersionedValue<V>>>();
        for(RemoteCache<K,V> cache : quorumCache()){
            futures.add(cache.getVersionedAsynch(k));
        }

        VersionedValue<V> ret = null;
        for(NotifyingFuture<VersionedValue<V>> future : futures){
            VersionedValue<V> tmp = null;
            try {
                tmp = future.get();
            } catch (InterruptedException e) {
                e.printStackTrace();  // TODO: Customise this generated block
            } catch (ExecutionException e) {
                e.printStackTrace();  // TODO: Customise this generated block
            }
            if(ret == null){
                ret = tmp;
            }else{
                if(tmp !=  null && tmp.getVersion() > ret.getVersion())
                    ret = tmp;
            }
        }
        return ret;
    }

}
