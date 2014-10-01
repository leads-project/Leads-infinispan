package org.infinispan.ensemble.test;

import org.infinispan.client.hotrod.TestHelper;
import org.infinispan.client.hotrod.test.HotRodClientTestingUtil;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.ensemble.EnsembleCacheManager;
import org.infinispan.ensemble.indexing.LocalIndexBuilder;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.query.remote.client.avro.AvroMarshaller;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.fwk.TransportFlags;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;

import java.util.ArrayList;
import java.util.List;

import static org.infinispan.server.hotrod.test.HotRodTestingUtil.hotRodCacheConfiguration;
import static org.infinispan.test.TestingUtil.blockUntilCacheStatusAchieved;


/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public abstract class EnsembleCacheAbstractTest<T> extends MultipleCacheManagersTest {

    // Server side
    private List<HotRodServer> servers = new ArrayList<>();

    // Client side
    private List<String> sites = new ArrayList<>();
    protected EnsembleCacheManager manager;

    // Parameters
    protected abstract String cacheName();
    protected abstract int numberOfSites();
    protected abstract Class<? extends T> beanClass();


    @Override
    protected void createCacheManagers() throws Throwable {

        ConfigurationBuilder builder = hotRodCacheConfiguration(getDefaultClusteredCacheConfig(CacheMode.REPL_SYNC, false));
        builder.indexing().enable()
                .addProperty("default.directory_provider", "ram")
                .addProperty("lucene_version", "LUCENE_CURRENT");

        createSites(numberOfSites(), builder);

        manager = new EnsembleCacheManager(sites,new AvroMarshaller<>(beanClass()),new LocalIndexBuilder());

    }

    @AfterMethod(alwaysRun = true)
    protected void clearContent() throws Throwable {
        // Do not clear content to allow servers
        // to stop gracefully and catch any issues there.
    }

    @AfterClass(alwaysRun = true)
    @Override
    public void destroy(){
        // Correct order is to stop servers first
        try {
            for (HotRodServer server : servers)
                HotRodClientTestingUtil.killServers(server);
        } finally {
            // And then the caches and cache managers
            super.destroy();
        }
        manager.stop();
    }

    private void createSites(int num, ConfigurationBuilder defaultBuilder) {
        // Start Hot Rod servers
        for (int i = 0; i < num; i++) addHotRodServer(defaultBuilder);
        // Verify that default caches should be started
        for (int i = 0; i < num; i++) assert manager(i).getCache() != null;
        // Verify that caches running
        for (int i = 0; i < num; i++) {
            blockUntilCacheStatusAchieved(
                    manager(i).getCache(), ComponentStatus.RUNNING, 10000);
        }
        for (int i = 0; i < num; i++)
            sites.add(server(i).getHost()+":"+server(i).getPort());
    }

    private HotRodServer server(int i) {
        return servers.get(i);
    }

    private HotRodServer addHotRodServer(ConfigurationBuilder builder) {
        TransportFlags transportFlags = new TransportFlags();
        transportFlags.withMerge(false);
        EmbeddedCacheManager cm = addClusterEnabledCacheManager(builder,transportFlags);
        cm.defineConfiguration(cacheName(), builder.build());
        HotRodServer server = TestHelper.startHotRodServer(cm);
        servers.add(server);
        return server;
    }

}
