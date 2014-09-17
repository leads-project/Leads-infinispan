package org.infinispan.ensemble;

import org.infinispan.commons.CacheException;
import org.infinispan.commons.api.BasicCacheContainer;
import org.infinispan.ensemble.cache.*;
import org.infinispan.ensemble.cache.distributed.DistributedEnsembleCache;
import org.infinispan.ensemble.cache.distributed.Partitioner;
import org.infinispan.ensemble.cache.replicated.SWMREnsembleCache;
import org.infinispan.ensemble.cache.replicated.MWMREnsembleCache;
import org.infinispan.ensemble.cache.replicated.WeakEnsembleCache;
import org.infinispan.ensemble.indexing.IndexBuilder;
import org.infinispan.ensemble.indexing.LocalIndexBuilder;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.util.*;
import java.util.concurrent.ConcurrentMap;


/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public class EnsembleCacheManager implements  BasicCacheContainer{

    private static final Log log = LogFactory.getLog(EnsembleCacheManager.class);

    public static int DEFAULT_REPLICATION_FACTOR = 1;
    public static final int ENSEMBLE_VERSION_MAJOR = 0;
    public static final int ENSEMBLE_VERSION_MINOR = 2;
    public static enum Consistency {
        SWMR,
        MWMR,
        WEAK
    }

    private ConcurrentMap<String, Site> sites;
    private ConcurrentMap<String, EnsembleCache> caches;


    public EnsembleCacheManager() throws CacheException{
        this(Collections.EMPTY_LIST);
    }

    public EnsembleCacheManager(String siteList) throws CacheException{
        this(Arrays.asList(siteList.split("\\|")));
    }

    /**
     *
     * Create an EnsembleCacheManager using <i>sites</i> as the set of sites, and a local IndexBuilder..
     * By convention, the first site is local.
     *
     * @param sites
     * @throws CacheException
     */
    public EnsembleCacheManager(Collection<String> sites) throws CacheException{
        this(Site.fromNames(sites),new LocalIndexBuilder());
    }

    public EnsembleCacheManager(Collection<Site> sites, IndexBuilder indexBuilder) throws CacheException{
        this.caches = indexBuilder.getIndex(EnsembleCache.class);
        this.sites = indexBuilder.getIndex(Site.class);
        for(Site s : sites){
            this.sites.put(s.getName(),s);
        }
    }

    //
    // SITE MANAGEMENT
    //

    public  boolean addSite(Site site){
        return sites.putIfAbsent(site.getName(),site)==null;
    }

    public boolean removeSite(Site site){
        return sites.remove(site)==null;
    }

    public Collection<Site> sites(){
        return sites.values();
    }

    public Site getSite(String name){
        return sites.get(name);
    }

    //
    // CACHE MANAGEMENT
    //

    /**
     * {@inheritDoc}
     **/
    @Override
    public <K, V> EnsembleCache<K, V> getCache() {
        return getCache(DEFAULT_CACHE_NAME);
    }

    /**
     * {@inheritDoc}
     **/
    @Override
    public <K,V> EnsembleCache<K,V> getCache(String cacheName){
        return getCache(cacheName, DEFAULT_REPLICATION_FACTOR);
    }

    public <K,V> EnsembleCache<K,V> getCache(String cacheName, int replicationFactor){
        return getCache(cacheName,assignRandomly(replicationFactor));
    }

    public <K,V> EnsembleCache<K,V> getCache(String cacheName, int replicationFactor, Consistency consistency){
        return getCache(cacheName,assignRandomly(replicationFactor),consistency);
    }

    public <K,V> EnsembleCache<K,V> getCache(String cacheName, List<Site> siteList){
        return getCache(cacheName,siteList,Consistency.WEAK);
    }

    public <K,V> EnsembleCache<K,V> getCache(String cacheName, List<Site> siteList, Consistency consistency){
        List<EnsembleCache<K,V>> cacheList = new ArrayList<EnsembleCache<K, V>>();
        for(Site s : siteList){
            cacheList.add(s.<K,V>getCache(cacheName));
        }
        return getCache(cacheName,cacheList,consistency,null);
    }

    public <K,V> EnsembleCache<K,V> getCache(String cacheName, List<EnsembleCache<K,V>> cacheList, Consistency consistency, Partitioner<K,V> partitioner){
        EnsembleCache<K,V> ret;
        if (partitioner==null){
            switch (consistency){
                case SWMR:
                    ret = new SWMREnsembleCache<K,V>(cacheName, cacheList);
                    break;
                case MWMR:
                    ret = new MWMREnsembleCache<K,V>(cacheName, cacheList);
                    break;
                case WEAK:
                    ret = new WeakEnsembleCache<K,V>(cacheName,cacheList);
                    break;
                default:
                    throw new CacheException("Invalid consistency level "+consistency.toString());
            }
        }else{
            ret = new DistributedEnsembleCache<K, V>(cacheName,cacheList,partitioner);
        }
        caches.putIfAbsent(cacheName,ret);
        return caches.get(cacheName);
    }


    //
    // OTHER METHODS
    //

    public void clear(){
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    //
    // HELPERS
    //

    /**
     *
     * @return a random list of <i>replicationFactor</i> sites..
     */
    private List<Site> assignRandomly(int replicationFactor){
        assert  replicationFactor <= sites.size() :sites.values().toString();
        List<Site> replicas = new ArrayList<Site>();
        Set<Site> all = new HashSet<Site>(sites.values());
        // First add local site
        if(Site.localSite()!=null)
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
