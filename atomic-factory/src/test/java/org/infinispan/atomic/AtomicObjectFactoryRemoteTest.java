package org.infinispan.atomic;

import org.infinispan.atomic.filter.FilterConverterFactory;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.api.BasicCacheContainer;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Transport;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.test.HotRodTestingUtil;
import org.infinispan.test.fwk.TransportFlags;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.infinispan.test.TestingUtil.blockUntilCacheStatusAchieved;

/**
 * @author Pierre Sutra
 */
public class AtomicObjectFactoryRemoteTest extends AtomicObjectFactoryAbstractTest{

   private static List<HotRodServer> servers = new ArrayList<>();
   private static List<BasicCacheContainer> remoteCacheManagers = new ArrayList<>();
   
   @Override 
   public BasicCacheContainer container(int i) {
      return remoteCacheManagers.get(i);
   }

   @Override 
   public Collection<BasicCacheContainer> containers() {
      return remoteCacheManagers;
   }

   @Override 
   protected void createCacheManagers() throws Throwable {
      
      ConfigurationBuilder defaultBuilder =getDefaultClusteredCacheConfig(CACHE_MODE, USE_TRANSACTIONS);
      
      defaultBuilder
            .clustering().
            cacheMode(CacheMode.DIST_SYNC)
            .hash()
            .numOwners(REPLICATION_FACTOR)
            .compatibility()
            .enable();
      
      for (int j = 0; j < NMANAGERS; j++) {
         GlobalConfigurationBuilder gbuilder = GlobalConfigurationBuilder.defaultClusteredBuilder();
         Transport transport = gbuilder.transport().getTransport();
         gbuilder.transport().transport(transport);
         startHotRodServer(gbuilder, defaultBuilder, j + 1);
         RemoteCacheManager manager = new RemoteCacheManager(
               new org.infinispan.client.hotrod.configuration.ConfigurationBuilder()
                     .addServers(
                           servers.get(j).getHost()+":"+servers.get(j).getPort())
                     .marshaller((Marshaller) null)
                     .build());
         remoteCacheManagers.add(manager);
      }
      
      // Verify that default caches are started.
      for (int j = 0; j < NMANAGERS; j++) {
         blockUntilCacheStatusAchieved(
               manager(j).getCache(), ComponentStatus.RUNNING, 10000);
      }
      
   }

   private void startHotRodServer(GlobalConfigurationBuilder gbuilder, ConfigurationBuilder builder, int nodeIndex) {
      TransportFlags transportFlags = new TransportFlags();
      EmbeddedCacheManager cm = addClusterEnabledCacheManager(gbuilder, builder, transportFlags);
      HotRodServer server = HotRodTestingUtil.startHotRodServer(cm,11222+nodeIndex);
      server.addCacheEventFilterConverterFactory(FilterConverterFactory.FACTORY_NAME, new FilterConverterFactory());
      server.startDefaultCache();
      servers.add(server);
   }

}
