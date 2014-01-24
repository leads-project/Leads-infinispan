package org.infinispan.ensemble.test;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.test.MultiHotRodServersTest;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.ensemble.EnsembleCacheManager;
import org.infinispan.ensemble.Site;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.infinispan.server.hotrod.test.HotRodTestingUtil.hotRodCacheConfiguration;
import static org.infinispan.ensemble.EnsembleCacheManager.Consistency.STRONG;

/**
  *
 * @author Pierre Sutra
 * @since 6.0
 */
@Test(groups = "functional", testName = "ensemble.AtomicObjectFactoryTest")
public class EnsembleBasicTest extends MultiHotRodServersTest {

    private static int NCACHES = 3;
    private static String WEAK_CACHE_NAME = "georeplicatedWeakCache";
    private static String STRONG_CACHE_NAME = "georeplicatedSWMRCache";
    private static EnsembleCacheManager manager;

     public void basicWeakCacheUsageTest() throws  Exception {
        BasicCache cache = manager.getCache();
        cache.put("key", "smthing");
        assert cache.containsKey("key");
        assert cache.get("key").equals("smthing");
    }

    public void georeplicatedWeakCacheUsageTest() throws Exception {
        BasicCache cache = manager.getCache(WEAK_CACHE_NAME,2);
        cache.put("key","smthing");
        assert cache.containsKey("key");
        assert cache.get("key").equals("smthing");
    }

    public void basicStrongCacheUsageTest() throws  Exception {
        BasicCache cache = manager.getCache();
        cache.put("key", "smthing");
        assert cache.containsKey("key");
        assert cache.get("key").equals("smthing");
    }

    public void georeplicatedStrongCacheUsageTest() throws Exception {
        BasicCache cache = manager.getCache(STRONG_CACHE_NAME,2,STRONG);
        cache.put("key","smthing");
        assert cache.containsKey("key");
        assert cache.get("key").equals("smthing");
    }


    @Override
    protected void createCacheManagers() throws Throwable {
        ConfigurationBuilder builder = hotRodCacheConfiguration(getDefaultClusteredCacheConfig(CacheMode.REPL_SYNC, false));
        createHotRodServers(3 , builder);
        for(EmbeddedCacheManager m: cacheManagers){
            m.defineConfiguration(WEAK_CACHE_NAME, builder.build());
            m.defineConfiguration(STRONG_CACHE_NAME, builder.build());
        }

        List<Site> uclouds = new ArrayList<Site>();
        Iterator<RemoteCacheManager> it =  clients.iterator();
        Site cloud1 = new Site("neuchatel",it.next());
        Site cloud2 = new Site("dresen",it.next());
        Site cloud3 = new Site("chania",it.next());
        uclouds.add(cloud1);
        uclouds.add(cloud2);
        uclouds.add(cloud3);
        manager = new EnsembleCacheManager(uclouds);
    }

}
