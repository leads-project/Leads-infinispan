package org.infinispan.ensemble;

import org.apache.zookeeper.KeeperException;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;



/**
 *
 * Zk is deployed on ALL microclouds
 * Each ucloud has an address to which it always answer (via DNS) to ease configuration here
 *
 * FIXME add watcher on the modifications that occur to the index.
 *
 * Do we implement a RemotecacheContainer (yes, if the laod is too heavy for Zk).
 * What pattern to use for handling faults properly ?
 * It is also possible that the manger are found on the fly using Zk
 *
 *
 * @author otrack
 * @since 4.0
 */
public class EnsembleCacheManager implements  CacheContainer{

    private Map<String,EnsembleCache> ensembles;
    private Map<UCloud,CacheContainer> containers;
    private ConcurrentMap<String,List<UCloud>> index;
    private int replicationFactor;

    public EnsembleCacheManager(UCloud lucloud, List<UCloud> uclouds, int replicationFactor) throws IOException, KeeperException, InterruptedException {
        assert replicationFactor > uclouds.size();

        ensembles = new HashMap<String, EnsembleCache>();
        this.containers = new HashMap<UCloud, CacheContainer>();
        this.replicationFactor = replicationFactor;
        for(UCloud ucloud : uclouds){
            this.containers.put(ucloud, ucloud.getContainer());
        }
        this.index = ZkManager.getInstance().newConcurrentMap("/index");
    }

    @Override
    public <K, V> Cache<K, V> getCache() {
        return getCache("");
    }

    /**
     * Retrieve a cache object from the shared index.
     *
     * @param cacheName
     * @param <K>
     * @param <V>
     * @return null if no cache exists.
     */
    public <K,V> Cache<K,V> getCache(String cacheName){

        if(ensembles.containsKey(cacheName))
            return ensembles.get(cacheName);

        List<UCloud> uClouds = index.get(cacheName);
        if(uClouds==null){
            uClouds = assignRandomly();
            index.put(cacheName,uClouds);
        }

        return getCache(cacheName,uClouds);
    }

    /**
     *
     * Create a cache object in the shared index.
     * The object is retrieved in case, it was already existing.
     *
     * @param cacheName
     * @param uclouds
     * @param <K>
     * @param <V>
     * @return an EnsembleCache with name <i>cacheName</i>backed by RemoteCaches on <i>uclouds</i>.
     */
    public <K,V> EnsembleCache<K,V> getCache(String cacheName, List<UCloud> uclouds) {

        if(ensembles.containsKey(cacheName))
            return ensembles.get(cacheName);

        List<UCloud> uClouds = index.get(cacheName);
        if(uClouds==null)
            return null;

        List<ConcurrentMap<K,V>> caches = new ArrayList<ConcurrentMap<K,V>>();
        for(UCloud ucloud: uClouds){
            caches.add((Cache<K, V>) containers.get(ucloud).getCache(cacheName));
        }

        EnsembleCache e = new EnsembleCache<K, V>(cacheName, caches);
        ensembles.put(cacheName, e);
        return e;

    }

    /**
     *
     * Remove the cache object from the shared index.
     *
     * @param cache
     * @param <K>
     * @param <V>
     * @return
     */
    public <K,V> boolean removeCache(EnsembleCache<K,V> cache){
        return index.remove(cache.getName()) == null;
    }


    //
    // INNER METHIDS
    //

    private UCloud retrieveUCloudByName(String name){
        for(UCloud ucloud : containers.keySet()){
            if(ucloud.getName().equals(name)) return  ucloud;
        }
        return null;
    }

    /**
     *
     * @return a list of <i>replicationFactor</i> uclouds.
     */
    private List<UCloud> assignRandomly(){
        List<UCloud> uclouds = new ArrayList<UCloud>(containers.keySet());
        java.util.Collections.shuffle(uclouds);
        for(int i=uclouds.size()-replicationFactor;i>0;i--)
            uclouds.remove(0);
        return uclouds;
    }

    @Override
    public void start() {
        // TODO: Customise this generated block
    }

    @Override
    public void stop() {
        // TODO: Customise this generated block
    }
}
