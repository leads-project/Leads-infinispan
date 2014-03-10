package org.infinispan.ensemble;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.CacheException;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.api.BasicCacheContainer;
import org.menagerie.DefaultZkSessionManager;
import org.menagerie.ZkSessionManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
        SWMR,
        MWMR,
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

    public EnsembleCacheManager(List<String> sites, String zkConnectString, int to) throws CacheException{

        try {
            zkManager = new DefaultZkSessionManager(zkConnectString,to);
            // this.index = new ZkHashMap<String, List<Site>>(ZK_INDEX,zkManager,new MenagerieSerializer<Map.Entry<String, List<Site>>>());
            this.index = new ConcurrentHashMap<String, List<Site>>();
            // this.sites = new ZkListSet<Site>(ZK_SITES,zkManager,new MenagerieSerializer<Site>());
            this.sites = new HashSet<Site>();
        } catch (Exception e) {
            throw new CacheException("Cannot connect to Zk; reason = "+e.getMessage());
        }

        this.ensembles = new HashMap<String, EnsembleCache>();

        // By convention, the first site is local
        boolean local=true;
        for(String s: sites){
            Site site = new Site(s, new RemoteCacheManager(s),local);
            if(local) local=false;
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
        return getCache(cacheName,assignRandomly(replicationFactor),consistency);
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
            System.out.println("Cache already exists (local)");
            return (BasicCache<K,V>) ensembles.get(cacheName);
        }

        List<Site> previous = index.putIfAbsent(cacheName,sites);
        if(previous != null){
            System.out.println("Cache already exists (remote)");
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
            case SWMR:
                e = new SWMREnsembleCache<K,V>(cacheName, caches);
                break;
            case MWMR:
                e = new MWMREnsembleCache<K,V>(cacheName, caches);
                e.put(null,null);
                break;
            case WEAK:
                e = new WeakEnsembleCache<K,V>(cacheName,caches);
                break;
            default:
                throw new CacheException("Invalid consistency level "+consistency.toString());
        }

        ensembles.put(cacheName, e);
        return e;
    }

    public synchronized boolean addSite(Site site){
        boolean ret = sites.add(site);
        System.out.println("Site added : "+ret);
        return ret;
    }

    /**
     *
     * @return the available sites in the current state.
     */
    public synchronized Set<Site> sites(){
        return new HashSet<Site>(sites);
    }

    public synchronized void clear(){
        System.out.println("Clearing Ensemble");
        for(String cache : index.keySet()){
            for(Site site : index.get(cache)){
                site.getManager().getCache(cache).clear();
            }
        }
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
        assert  replicationFactor <= Site._sites.size() : Site._sites.values().toString();
        List<Site> replicas = new ArrayList<Site>();
        Set<Site> all = new HashSet<Site>(Site._sites.values());
        // First add local site
        if(Site.localSite!=null)
            replicas.add(Site.localSite());
        // Then, complete
        for(Site s: all){
            if(replicas.size()==replicationFactor)
                break;
            if(!replicas.contains(s))
                replicas.add(s);
        }
        return replicas;
    }

}
