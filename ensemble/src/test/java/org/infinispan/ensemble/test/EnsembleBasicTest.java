package org.infinispan.ensemble.test;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.test.MultiHotRodServersTest;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.ensemble.EnsembleCacheManager;
import org.infinispan.ensemble.Site;
import org.infinispan.ensemble.cache.EnsembleCache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.util.Iterator;

import static org.infinispan.ensemble.EnsembleCacheManager.Consistency.SWMR;
import static org.infinispan.server.hotrod.test.HotRodTestingUtil.hotRodCacheConfiguration;


/**
  *
 * @author Pierre Sutra
 * @since 6.0
 */
@Test(groups = "functional", testName = "ensemble.AtomicObjectFactoryTest")
public class EnsembleBasicTest extends MultiHotRodServersTest {

    private static int NCACHES = 3;
    private static String WEAK_CACHE_NAME = "georeplicatedWeakCache";
    private static String SWMR_CACHE_NAME = "georeplicatedSWMRCache";
    private static EnsembleCacheManager manager;

    // replicated ensemble caches

    public void basicWeakEnsembleCacheTest() throws  Exception {
        EnsembleCache<String,String> cache = manager.getCache();
        cache.put("key", "smthing");
        assert cache.containsKey("key");
        assert cache.get("key").equals("smthing");
    }

    public void basicSWMREnsembleCacheTest() throws  Exception {
        EnsembleCache<String,String> cache = manager.getCache();
        cache.put("key", "smthing");
        assert cache.containsKey("key");
        assert cache.get("key").equals("smthing");
    }

    public void SWMRCacheUsageTest() throws Exception {
        EnsembleCache<String,String> cache = manager.getCache(SWMR_CACHE_NAME,2,SWMR);
        cache.put("key","smthing");
        assert cache.containsKey("key");
        assert cache.get("key").equals("smthing");
    }

    public void WeakCacheUsageTest() throws Exception {
        EnsembleCache<String,String> cache = manager.getCache(WEAK_CACHE_NAME,2);
        cache.put("key","smthing");
        assert cache.containsKey("key");
        assert cache.get("key").equals("smthing");
    }

    // distributed ensemble caches

    public void distributedEnsembleCacheTest(){

    }


    @Override
    protected void createCacheManagers() throws Throwable {
        ConfigurationBuilder builder = hotRodCacheConfiguration(getDefaultClusteredCacheConfig(CacheMode.REPL_SYNC, false));
        createHotRodServers(NCACHES, builder);
        for(EmbeddedCacheManager m: cacheManagers){
            m.defineConfiguration(WEAK_CACHE_NAME, builder.build());
            m.defineConfiguration(SWMR_CACHE_NAME, builder.build());
        }

        manager = new EnsembleCacheManager();
        Iterator<RemoteCacheManager> it =  clients.iterator();
        manager.addSite(new Site("neuchatel",it.next(),false));
        manager.addSite(new Site("dresden",it.next(),false));
        manager.addSite(new Site("chania",it.next(),false));
     }


    @AfterClass(alwaysRun = true)
    @Override
    public void destroy(){
        super.destroy();
        manager.stop();
    }

}
