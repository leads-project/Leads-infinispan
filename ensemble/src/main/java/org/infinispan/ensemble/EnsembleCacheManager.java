package org.infinispan.ensemble;

import org.apache.zookeeper.KeeperException;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.api.BasicCacheContainer;
import org.infinispan.ensemble.zk.ZkManager;

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
 * @author Pierre Sutra
 * @since 6.0
 */
public class EnsembleCacheManager implements  BasicCacheContainer{

    public static enum Consistency { WEAK, SWMR,  MWMR}
    public static final int ENSEMBLE_VERSION_MAJOR = 0;
    public static final int ENSEMBLE_VERSION_MINOR = 1;

    private ZkManager zkManager;
    private Map<String,EnsembleCache> ensembles;
    private Map<Site,BasicCacheContainer> containers;
    private ConcurrentMap<String,List<Site>> index;

    public EnsembleCacheManager(List<Site> sites)
            throws IOException, KeeperException, InterruptedException {
        init(sites,"127.0.0.1","2181");
    }

    public EnsembleCacheManager(List<String> hosts, String zhost, String zport)
            throws IOException, KeeperException, InterruptedException {
        List<Site> sites = new ArrayList<Site>();
        for(String host: hosts){
            sites.add(new Site(host, new RemoteCacheManager(host+":11222")));
        }
        init(sites, zhost, zport);
    }

    @Override
    public synchronized <K, V> BasicCache<K, V> getCache() {

        if(ensembles.containsKey(DEFAULT_CACHE_NAME))
            return ensembles.get(DEFAULT_CACHE_NAME);

        return getCache(DEFAULT_CACHE_NAME);
    }

    /**
     * Retrieve a cache object from the shared index.
     *
     * @param cacheName
     * @param <K>
     * @param <V>
     * @return null if no cache exists.
     */
    @Override
    public synchronized <K,V> BasicCache<K,V> getCache(String cacheName){

        if(ensembles.containsKey(cacheName))
            return (BasicCache<K,V>) ensembles.get(cacheName);

        return getCache(cacheName,1);
    }

    public synchronized <K,V> BasicCache<K,V> getCache(String cacheName, int replicationFactor){

        if(ensembles.containsKey(cacheName))
            return (BasicCache<K,V>) ensembles.get(cacheName);

        return getCache(cacheName,assignRandomly(replicationFactor),Consistency.WEAK);
    }

    public synchronized <K,V> BasicCache<K,V> getCache(String cacheName, int replicationFactor, Consistency consistency){

        if(ensembles.containsKey(cacheName))
            return (BasicCache<K,V>) ensembles.get(cacheName);

        return getCache(cacheName,assignRandomly(replicationFactor),Consistency.WEAK);
    }


    /**
     *
     * Create or retrieve a cache object in the shared index with an explicit data placement.
     * The object is retrieved in case, it was already existing.
     *
     * @param cacheName
     * @param uclouds
     * @param <K>
     * @param <V>
     * @return an EnsembleCache with name <i>cacheName</i>backed by RemoteCaches on <i>uclouds</i>.
     */
    public synchronized <K,V> BasicCache<K,V> getCache(String cacheName, List<Site> uclouds, Consistency consistency) {

        if(ensembles.containsKey(cacheName))
            return (BasicCache<K,V>) ensembles.get(cacheName);

        // TODO concurrency
        index.put(cacheName,uclouds);

        List<BasicCache<K,V>> caches = new ArrayList<BasicCache<K, V>>();
        for(Site ucloud: uclouds){
            caches.add((BasicCache<K, V>) containers.get(ucloud).getCache(cacheName));
        }

        EnsembleCache e  = null;
        switch (consistency){
            case WEAK:
                e = new WeakEnsembleCache(cacheName,caches);
                break;
            case SWMR:
                e = new SWMREnsembleCache(cacheName, caches);
                break;
            default:
                // e = new MWMREnsemblecache(cacheName, caches);
                break;

        }

        ensembles.put(cacheName, e);
        return e;
    }


    //
    // INNER METHIDS
       
    private void init(List<Site> sites, String zhost, String zport)
            throws IOException {
        ensembles = new HashMap<String, EnsembleCache>();
        this.containers = new HashMap<Site, BasicCacheContainer>();
        for(Site ucloud : sites){
            this.containers.put(ucloud, ucloud.getContainer());
        }

        this.zkManager = new ZkManager(zhost,zport);
        this.index = zkManager.newConcurrentMap("/index");
    }

    /**
     *
     * @return a list of <i>replicationFactor</i> uclouds.
     */
    private List<Site> assignRandomly(int replicationFactor){
        List<Site> uclouds = new ArrayList<Site>(containers.keySet());
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
