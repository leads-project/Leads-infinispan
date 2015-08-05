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
   private static List<EmbeddedCacheManager> cacheManagers = new ArrayList<>();
   private static List<BasicCacheContainer> remoteCacheManagers = new ArrayList<>();
   private static ConfigurationBuilder defaultBuilder;
   private static GlobalConfigurationBuilder globalBuilder;

   @Override 
   public BasicCacheContainer container(int i) {
      return remoteCacheManagers.get(i);
   }

   @Override 
   public Collection<BasicCacheContainer> containers() {
      return remoteCacheManagers;
   }

   @Override
   public boolean addContainer() {
      HotRodServer server = addHotRodServer(globalBuilder, defaultBuilder);
      RemoteCacheManager manager = new RemoteCacheManager(
            new org.infinispan.client.hotrod.configuration.ConfigurationBuilder()
                  .addServers(server.getHost()+":"+server.getPort())
                  .marshaller((Marshaller) null)
                  .build());
      remoteCacheManagers.add(manager);
      return true;
   }

   @Override
   public  boolean deleteContainer() {
      if (servers.size()==0) return false;
      servers.get(servers.size() - 1).stop();
      servers.remove(servers.size()-1);
      cacheManagers.get(cacheManagers.size()-1).stop();
      cacheManagers.remove(cacheManagers.size()-1);
      return true;
   }


   @Override 
   protected void createCacheManagers() throws Throwable {
      createDefaultBuilder();
      createGlobalConfigurationBuilder();

      for (int j = 0; j < NMANAGERS; j++) {
         addContainer();
      }

      // Verify that default caches are started.
      for (int j = 0; j < NMANAGERS; j++) {
         blockUntilCacheStatusAchieved(
               manager(j).getCache(), ComponentStatus.RUNNING, 10000);
      }

      AtomicObjectFactory.forCache(cache(0));
   }

   private HotRodServer addHotRodServer(GlobalConfigurationBuilder gbuilder, ConfigurationBuilder builder) {
      int nodeIndex = servers.size();
      TransportFlags transportFlags = new TransportFlags();
      EmbeddedCacheManager cm = addClusterEnabledCacheManager(gbuilder, builder, transportFlags);
      cacheManagers.add(cm);
      HotRodServer server = HotRodTestingUtil.startHotRodServer(cm,11222+nodeIndex);
      server.addCacheEventFilterConverterFactory(FilterConverterFactory.FACTORY_NAME, new FilterConverterFactory());
      server.startDefaultCache();
      servers.add(server);
      return server;
   }

   private void createDefaultBuilder() {
      defaultBuilder =getDefaultClusteredCacheConfig(CACHE_MODE, USE_TRANSACTIONS);
      defaultBuilder
            .clustering().
            cacheMode(CacheMode.DIST_SYNC)
            .hash()
            .numOwners(REPLICATION_FACTOR)
            .locking().useLockStriping(false)
            .compatibility().enable();
   }

   private void createGlobalConfigurationBuilder(){
      globalBuilder = GlobalConfigurationBuilder.defaultClusteredBuilder();
      Transport transport = globalBuilder.transport().getTransport();
      globalBuilder.transport().transport(transport);
   }

}
