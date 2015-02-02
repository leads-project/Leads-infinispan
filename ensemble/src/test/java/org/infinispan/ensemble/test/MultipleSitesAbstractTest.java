package org.infinispan.ensemble.test;

import org.infinispan.client.hotrod.TestHelper;
import org.infinispan.client.hotrod.test.HotRodClientTestingUtil;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.Index;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Transport;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.fwk.TransportFlags;
import org.infinispan.transaction.TransactionMode;
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

   // total numbder of sites
   protected abstract int numberOfSites();

   // node per site
   protected abstract int numberOfNodes();

   @Override
   protected void createCacheManagers() throws Throwable {
      ConfigurationBuilder builder = hotRodCacheConfiguration(getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC, false));
      // builder.dataContainer().valueEquivalence(AnyEquivalence.getInstance());
      createSites(numberOfSites(), numberOfNodes(), builder);
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

   private void createSites(int nsites, int nnodes, ConfigurationBuilder defaultBuilder) {

      // Start Hot Rod servers at each site.
      for (int i = 0; i < nsites; i++) {
         for (int j = 0; j < nnodes; j++) {
            GlobalConfigurationBuilder gbuilder = GlobalConfigurationBuilder.defaultClusteredBuilder();
            Transport transport = gbuilder.transport().getTransport();
            gbuilder.transport().transport(transport);
            gbuilder.transport().clusterName("site-" + Integer.toString(i));
            startHotRodServer(gbuilder, defaultBuilder, i + 1, j + 1);
         }
      }

      // Create appropriate caches at each node.
      ConfigurationBuilder builder = hotRodCacheConfiguration(getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC, false));
      builder.indexing()
            .enable()
            .index(Index.LOCAL)
            .addProperty("default.directory_provider", "ram")
            .addProperty("lucene_version", "LUCENE_CURRENT");
      builder.clustering().hash().numOwners(1);
      builder.jmxStatistics().enable();
      builder.transaction().transactionMode(TransactionMode.TRANSACTIONAL);
      Configuration configuration = builder.build();
      for (int i = 0; i < nsites; i++) {
         for (int j = 0; j < nnodes; j++) {
            for (String name : cacheNames()) {
               manager(i*nnodes+j).defineConfiguration(name,configuration);
               manager(i * nnodes + j).getCache(name, true);
            }
         }
      }

      // Verify that default caches are started.
      for (int i = 0; i < nsites; i++) {
         for (int j = 0; j < nnodes; j++) {
            assert manager(i*nnodes+j).getCache() != null;
         }
      }

      // Verify that the default caches is running.
      for (int i = 0; i < nsites; i++) {
         for (int j = 0; j < nnodes; j++) {
            blockUntilCacheStatusAchieved(
                  manager(i*nnodes+j).getCache(), ComponentStatus.RUNNING, 10000);
         }
      }

      for (int i = 0; i < nsites; i++) {
         String site ="";
         for (int j = 0; j < nnodes; j++) {
            site += server(i*nnodes+j).getHost() + ":" + server(i*nnodes+j).getPort()+",";
         }
         site = site.substring(0,site.length()-1);
         sites.add(site);
      }
   }

   private HotRodServer server(int i) {
      return servers.get(i);
   }

   private void startHotRodServer(GlobalConfigurationBuilder gbuilder, ConfigurationBuilder builder, int siteIndex, int nodeIndex) {
      TransportFlags transportFlags = new TransportFlags();
      transportFlags.withSiteIndex(siteIndex);
      transportFlags.withNodeIndex(nodeIndex);
      transportFlags.withReplay2(false);
      EmbeddedCacheManager cm = addClusterEnabledCacheManager(gbuilder, builder, transportFlags);
      HotRodServer server = TestHelper.startHotRodServer(cm);
      servers.add(server);
   }
}
