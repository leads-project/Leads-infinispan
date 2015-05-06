package org.infinispan.atomic.utils;

import org.infinispan.atomic.filter.FilterConverterFactory;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Transport;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.test.fwk.TransportFlags;

import static org.infinispan.test.AbstractCacheTest.getDefaultClusteredCacheConfig;


/**
 * @author Pierre Sutra
 */
public class Server {

   protected static int REPLICATION_FACTOR = 1;
   protected static CacheMode CACHE_MODE = CacheMode.DIST_SYNC;
   protected static boolean USE_TRANSACTIONS = false;
   private static String host ="localhost";
   private static String proxyhost ="localhost";

   public static void main(String args[]) {

      ConfigurationBuilder defaultBuilder = getDefaultClusteredCacheConfig(CACHE_MODE, USE_TRANSACTIONS);
      
      if (args.length>0)
         host = args[0];

      if (args.length>1)
         proxyhost = args[1];
      
      defaultBuilder
            .clustering().
            cacheMode(CacheMode.DIST_SYNC)
            .hash()
            .numOwners(REPLICATION_FACTOR)
            .compatibility().enable();

      GlobalConfigurationBuilder gbuilder = GlobalConfigurationBuilder.defaultClusteredBuilder();
      Transport transport = gbuilder.transport().getTransport();
      gbuilder.transport().transport(transport);
      startHotRodServer(gbuilder, defaultBuilder, 0);
      System.out.println("LAUNCHED");
      try {
         Thread.sleep(10000000);
      } catch (InterruptedException e) {
         e.printStackTrace();  // TODO: Customise this generated block
      }

   }

   private static HotRodServer startHotRodServer(GlobalConfigurationBuilder gbuilder, ConfigurationBuilder builder, int nodeIndex) {
      int port = 11222+nodeIndex;
      TransportFlags transportFlags = new TransportFlags();
      EmbeddedCacheManager cm = TestCacheManagerFactory.createClusteredCacheManager(gbuilder, builder, transportFlags);
      HotRodServerConfigurationBuilder hbuilder = new HotRodServerConfigurationBuilder();
      hbuilder.topologyStateTransfer(false);      
      hbuilder.proxyHost(proxyhost);
      hbuilder.host(host);
      hbuilder.port(port);
      
      HotRodServer server = new HotRodServer();
      server.start(hbuilder.build(),cm);
      server.addCacheEventFilterConverterFactory(FilterConverterFactory.FACTORY_NAME, new FilterConverterFactory());
      return server;
   }


}
