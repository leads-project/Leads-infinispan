package org.infinispan.ensemble;

import org.infinispan.commons.api.BasicCache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.fwk.TransportFlags;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
  *
 * @author Pierre Sutra
 * @since 6.0
 */
@Test(groups = "functional", testName = "ensemble.AtomicObjectFactoryTest")
public class EnsembleBasicTest extends MultipleCacheManagersTest {

    private static int NCACHES = 4;

    public void basicUsageTest() throws  Exception {
        Iterator<EmbeddedCacheManager> it =  cacheManagers.iterator();
        Site cloud1 = new Site("neuchatel",it.next());
        Site cloud2 = new Site("dresen",it.next());
        Site cloud3 = new Site("chania",it.next());
        List<Site> uclouds = new ArrayList<Site>();
        uclouds.add(cloud1);
        uclouds.add(cloud2);
        uclouds.add(cloud3);
        EnsembleCacheManager manager = new EnsembleCacheManager(uclouds,2);
        BasicCache cache = manager.getCache();
        cache.put("test","smthing");
        assert cache.containsKey("test");
        cache.remove("test", "smthing");
        assert !cache.containsKey("test");
    }

    @Override
    protected void createCacheManagers() throws Throwable {
        ConfigurationBuilder builder = getDefaultClusteredCacheConfig(CacheMode.REPL_SYNC, true);
        TransportFlags flags = new TransportFlags();
        createClusteredCaches(NCACHES, builder, flags);
    }

}
