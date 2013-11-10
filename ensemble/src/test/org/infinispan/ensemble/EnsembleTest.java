package org.infinispan.ensemble;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.fwk.TransportFlags;
import org.testng.annotations.Test;

/**
 * // TODO: Document this
 *
 * @author otrack
 * @since 4.0
 */
@Test(groups = "functional", testName = "distexec.AtomicObjectFactoryTest")
public class EnsembleTest extends MultipleCacheManagersTest {

    private static int NCACHES = 4;

    public void basicUsageTest() throws  Exception {
        UCloud cloud1 = new UCloud("neuchatel",cacheManagers.iterator().next());
        UCloud cloud2 = new UCloud("dresen",cacheManagers.iterator().next());

    }

    @Override
    protected void createCacheManagers() throws Throwable {
        ConfigurationBuilder builder = getDefaultClusteredCacheConfig(CacheMode.REPL_SYNC, true);
        TransportFlags flags = new TransportFlags();
        createClusteredCaches(NCACHES, builder, flags);
    }
}
