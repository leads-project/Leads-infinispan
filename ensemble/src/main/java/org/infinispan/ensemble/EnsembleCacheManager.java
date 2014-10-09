package org.infinispan.ensemble;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.CacheException;
import org.infinispan.commons.api.BasicCacheContainer;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.ensemble.cache.EnsembleCache;
import org.infinispan.ensemble.cache.distributed.DistributedEnsembleCache;
import org.infinispan.ensemble.cache.distributed.Partitioner;
import org.infinispan.ensemble.cache.replicated.MWMREnsembleCache;
import org.infinispan.ensemble.cache.replicated.SWMREnsembleCache;
import org.infinispan.ensemble.cache.replicated.WeakEnsembleCache;
import org.infinispan.ensemble.indexing.IndexBuilder;
import org.infinispan.ensemble.indexing.LocalIndexBuilder;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public class EnsembleCacheManager implements  BasicCacheContainer{

    private static final Log log = LogFactory.getLog(EnsembleCacheManager.class);

    public static int DEFAULT_REPLICATION_FACTOR = 1;
    public static enum Consistency {
        SWMR,
        MWMR,
        WEAK
    }

    private ConcurrentMap<String, Site> sites;
    private ConcurrentMap<String, EnsembleCache> caches;


    public EnsembleCacheManager() throws CacheException{
        this(Collections.EMPTY_LIST,null,new LocalIndexBuilder());
    }

    public EnsembleCacheManager(String siteList) throws CacheException{
        this(Arrays.asList(siteList.split("\\|")),null,new LocalIndexBuilder());
    }

    public EnsembleCacheManager(String siteList, Marshaller marshaller) throws CacheException{
        this(Arrays.asList(siteList.split("\\|")), marshaller, new LocalIndexBuilder());
    }

    /**
     *
     * Create an EnsembleCacheManager using <i>sites</i> as the set of sites, and a local IndexBuilder..
     * By convention, the first site is local.
     *
     * @param sites list of sites
     * @param marshaller marshaller to use (null is valid)
     * @param indexBuilder index to use
     * @throws CacheException
     */
    public EnsembleCacheManager(Collection<String> sites, Marshaller marshaller, IndexBuilder indexBuilder) throws CacheException{
        this.caches = indexBuilder.getIndex(EnsembleCache.class);
        this.sites = new ConcurrentHashMap<>();
        for(Site s : Site.valuesOf(sites, marshaller)){
            this.sites.put(s.getName(),s);
        }
    }

    public EnsembleCacheManager(Collection<RemoteCacheManager> managers, IndexBuilder indexBuilder) throws CacheException{
        this.caches = indexBuilder.getIndex(EnsembleCache.class);
        this.sites = new ConcurrentHashMap<>();
        boolean once = true;
        for(RemoteCacheManager m : managers){
            this.sites.put(
                    m.toString(),
                    new Site(m.toString(),m,once));
            if (once) once=false;
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
        EnsembleCache<K,V> ret = caches.get(cacheName);
        if (ret==null)
            ret = getCache(cacheName, DEFAULT_REPLICATION_FACTOR);
        return ret;
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
        List<EnsembleCache<K,V>> cacheList = new ArrayList<>();
        for(Site s : siteList){
            cacheList.add(s.<K,V>getCache(cacheName));
        }
        return getCache(cacheName,cacheList,consistency,true);
    }

    public <K,V> EnsembleCache<K,V> getCache(String cacheName, List<EnsembleCache<K,V>> cacheList, Consistency consistency, boolean create){
        EnsembleCache<K,V> ret;
        switch (consistency){
            case SWMR:
                ret = new SWMREnsembleCache<>(cacheName, cacheList);
                break;
            case MWMR:
                ret = new MWMREnsembleCache<>(cacheName, cacheList);
                break;
            case WEAK:
                ret = new WeakEnsembleCache<>(cacheName,cacheList);
                break;
            default:
                throw new CacheException("Invalid consistency level "+consistency.toString());
        }
        recordCache(ret,create);
        return caches.get(cacheName);
    }

    public <K,V> EnsembleCache<K,V> getCache(String cacheName, List<Site> siteList, boolean frontierMode, Partitioner<K,V> partitioner) {
        List<EnsembleCache<K,V>> cacheList = new ArrayList<>();
        for(Site s : siteList)
            cacheList.add(s.<K,V>getCache(cacheName));
        EnsembleCache<K,V> ret;
        ret = new DistributedEnsembleCache<>(cacheName,cacheList,partitioner,frontierMode);
        recordCache(ret,true);
        return caches.get(cacheName);
    }

    public <K,V> EnsembleCache<K,V> getCache(String cacheName, List<EnsembleCache<K,V>> cacheList, Partitioner<K,V> partitioner, boolean frontierMode){
        EnsembleCache<K,V> ret;
        ret = new DistributedEnsembleCache<>(cacheName,cacheList,partitioner,frontierMode);
        recordCache(ret,true);
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
        assert  replicationFactor <= sites.size() :sites.values().toString() +" vs " + replicationFactor;
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

    private void recordCache(EnsembleCache cache, boolean create){
        if (create && caches.containsKey(cache.getName()))
            throw new CacheException("Cache already existing");
        caches.putIfAbsent(cache.getName(),cache);
    }

}
