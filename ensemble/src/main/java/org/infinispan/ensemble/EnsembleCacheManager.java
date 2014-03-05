package org.infinispan.ensemble;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.CacheException;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.api.BasicCacheContainer;
import org.menagerie.DefaultZkSessionManager;
import org.menagerie.ZkSessionManager;
import org.menagerie.collections.ZkHashMap;
import org.menagerie.collections.ZkListSet;

import java.util.*;
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
 * It is also possible that the sites are found on the fly using Zk
 *
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public class EnsembleCacheManager implements  BasicCacheContainer{

    public static final int ENSEMBLE_VERSION_MAJOR = 0;
    public static final int ENSEMBLE_VERSION_MINOR = 1;

    public static enum Consistency {
        STRONG,
        WEAK
    }

    public static final String ZK_INDEX = "/index";
    public static final String ZK_SITES = "/sites";
    public static final int ZK_TO = 12000;


    private Map<String,EnsembleCache> ensembles;

    private ZkSessionManager zkManager;
    private ConcurrentMap<String,List<Site>> index;
    private Set<Site> sites;


    public EnsembleCacheManager() throws CacheException{
        this(Collections.EMPTY_LIST,"127.0.0.1",ZK_TO);
    }

    public EnsembleCacheManager(String sites) throws CacheException{
        this(Arrays.asList(sites.split("\\|")),"127.0.0.1:2181",ZK_TO);
    }

    public EnsembleCacheManager(String sites, String zkConnectString) throws CacheException{
        this(Arrays.asList(sites.split("\\|")),zkConnectString,ZK_TO);
    }

    public EnsembleCacheManager(List<String> sites, String zkHost, int to) throws CacheException{

        try {
            zkManager = new DefaultZkSessionManager(zkHost,to);
            this.index = new ZkHashMap<String, List<Site>>(ZK_INDEX,zkManager,new MenagerieSerializer<Map.Entry<String, List<Site>>>());
            this.sites = new ZkListSet<Site>(ZK_SITES,zkManager,new MenagerieSerializer<Site>());
        } catch (Exception e) {
            throw new CacheException("Cannot connect to Zk; reason = "+e.getMessage());
        }

        this.ensembles = new HashMap<String, EnsembleCache>();

        for(String s: sites){
            System.out.println("Creating site : "+s);
            Site site = new Site(s, new RemoteCacheManager(s));
            Site._sites.put(s,site);
            addSite(site);
        }

    }

    //
    // PUBLIC INTERFACE
    //

    /**
     * {@inheritDoc}
     **/
    @Override
    public synchronized <K, V> BasicCache<K, V> getCache() {

        if(ensembles.containsKey(DEFAULT_CACHE_NAME))
            return ensembles.get(DEFAULT_CACHE_NAME);

        return getCache(DEFAULT_CACHE_NAME);
    }

    /**
     * {@inheritDoc}
     **/
    @Override
    public <K,V> BasicCache<K,V> getCache(String cacheName){
        return getCache(cacheName,assignRandomly(1),Consistency.WEAK);
    }

    public <K,V> BasicCache<K,V> getCache(String cacheName, int replicationFactor){
        return getCache(cacheName,assignRandomly(replicationFactor),Consistency.WEAK);
    }

    public <K,V> BasicCache<K,V> getCache(String cacheName, int replicationFactor, Consistency consistency){
        return getCache(cacheName,assignRandomly(replicationFactor),Consistency.WEAK);
    }


    /**
     *
     * Create or retrieve a cache object in the shared index with an explicit data placement.
     * The object is retrieved in case, it was already existing.
     *
     * @param cacheName
     * @param sites
     * @param <K>
     * @param <V>
     * @return an EnsembleCache with name <i>cacheName</i>backed by RemoteCaches on <i>uclouds</i>.
     */
    public synchronized <K,V> BasicCache<K,V> getCache(String cacheName, List<Site> sites, Consistency consistency) {

        System.out.println("Creating cache : "+cacheName+" with ("+sites.toString()+";"+consistency+")");

        if(ensembles.containsKey(cacheName)){
            System.out.println("Cache exists");
            return (BasicCache<K,V>) ensembles.get(cacheName);
        }

        List<Site> previous = index.putIfAbsent(cacheName,sites);
        if(previous != null){
            sites = previous;
        }

        List<RemoteCache<K,V>> caches = new ArrayList<RemoteCache<K, V>>();
        for(Site site: sites){
            RemoteCache<K,V> c = site.getManager().getCache(cacheName);
            assert site != null;
            assert site.getManager() != null : site.getName();
            caches.add(c);
        }

        EnsembleCache e  = null;
        switch (consistency){
            case STRONG:
                e = new SWMREnsembleCache(cacheName, caches);
                break;
            default:
                e = new WeakEnsembleCache(cacheName,caches);
                break;
        }

        ensembles.put(cacheName, e);
        return e;
    }

    public synchronized void addSite(Site site){
        sites.add(site);
    }

    public void clear(){
        sites.clear();
        index.clear();
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        zkManager.shutdown();
    }




    //
    // INNER METHODS
    //

    /**
     *
     * @return a random list of <i>replicationFactor</i> uclouds.
     */
    private List<Site> assignRandomly(int replicationFactor){
        assert  replicationFactor < Site._sites.size();
        List<Site> replicas = new ArrayList<Site>(Site._sites.values());
        java.util.Collections.shuffle(replicas);
        for(int i=replicas.size()-replicationFactor;i>0;i--)
            replicas.remove(0);
        return replicas;
    }

}
