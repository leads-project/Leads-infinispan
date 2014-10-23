package org.infinispan.ensemble.test;

import org.infinispan.client.hotrod.TestHelper;
import org.infinispan.client.hotrod.test.HotRodClientTestingUtil;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Transport;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.fwk.TransportFlags;
import org.testng.annotations.AfterClass;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.EMPTY_LIST;
import static org.infinispan.server.hotrod.test.HotRodTestingUtil.hotRodCacheConfiguration;
import static org.infinispan.test.TestingUtil.blockUntilCacheStatusAchieved;


/**
 *
 * @author Pierre Sutra
 * @since 7.0
 */
public abstract class MultipleSitesAbstractTest extends MultipleCacheManagersTest {

    private List<HotRodServer> servers = new ArrayList<>();
    private List<String> sites = new ArrayList<>();

    protected abstract int numberOfSites();

    @Override
    protected void createCacheManagers() throws Throwable {
        ConfigurationBuilder builder = hotRodCacheConfiguration(getDefaultClusteredCacheConfig(CacheMode.REPL_SYNC, false));
        builder.indexing().enable()
                .addProperty("default.directory_provider", "ram")
                .addProperty("lucene_version", "LUCENE_CURRENT");
        builder.jmxStatistics().enable();
        createSites(numberOfSites(), builder);
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
    }

    public List<String> sites(){
        return sites;
    }

    public List<String> cacheNames(){
        return EMPTY_LIST;
    }

    public String connectionString(){
        String ret="";
        for (String site : sites)
            ret+=site+"|";
        return ret.substring(0,ret.length()-1);
    }

    // Helpers

    private void createSites(int num, ConfigurationBuilder defaultBuilder) {

        // Start Hot Rod servers at each site
        for (int i = 0; i < num; i++){
            GlobalConfigurationBuilder gbuilder = GlobalConfigurationBuilder.defaultClusteredBuilder();
            Transport transport = gbuilder.transport().getTransport();
            gbuilder.transport().transport(transport);
            gbuilder.transport().clusterName("site(" + Integer.toString(i)+ ")");
            startHotRodServer(gbuilder, defaultBuilder, i + 1);
        }

        // Create caches at each site
        for (int i = 0; i < num; i++){
            for(String name : cacheNames())
                manager(i).getCache(name,true);
        }

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

    private void startHotRodServer(GlobalConfigurationBuilder gbuilder, ConfigurationBuilder builder, int siteIndex) {
        TransportFlags transportFlags = new TransportFlags();
        transportFlags.withSiteIndex(siteIndex);
        transportFlags.withReplay2(false);
        EmbeddedCacheManager cm = addClusterEnabledCacheManager(gbuilder, builder, transportFlags);
        HotRodServer server = TestHelper.startHotRodServer(cm);
        servers.add(server);
    }
}
