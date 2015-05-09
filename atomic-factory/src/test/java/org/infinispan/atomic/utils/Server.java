package org.infinispan.atomic.utils;

import org.infinispan.atomic.filter.FilterConverterFactory;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.infinispan.test.AbstractCacheTest.getDefaultClusteredCacheConfig;

/**
 * @author Pierre Sutra
 */
public class Server implements Runnable {

   private static final String defaultHost ="localhost";
   
   private int REPLICATION_FACTOR = 1;
   private CacheMode CACHE_MODE = CacheMode.DIST_SYNC;
   private boolean USE_TRANSACTIONS = false;
   private String host;
   private String proxyhost;
   
   public Server (String host, String proxyhost) {
      this.host = host;
      this.proxyhost = proxyhost;
   }
   
   public static void main(String args[]) {

      String host = defaultHost;
      String proxyhost = defaultHost;
      
      if (args.length>0)
         host = args[0];

      if (args.length>1)
         proxyhost = args[1];
      
      ExecutorService executor = Executors.newSingleThreadExecutor();
      executor.execute(new Server(host, proxyhost));
      try {
         executor.shutdown();
      }catch (Exception e){
         // ignore
      }
      
   }

   @Override 
   public void run() {

      GlobalConfigurationBuilder gbuilder = GlobalConfigurationBuilder.defaultClusteredBuilder();
      gbuilder.transport().clusterName("aof-cluster");
      gbuilder.transport().nodeName("aof-server-"+host);

      ConfigurationBuilder builder= getDefaultClusteredCacheConfig(CACHE_MODE, USE_TRANSACTIONS);
      builder
            .clustering()
            .cacheMode(CacheMode.DIST_SYNC)
            .hash()
            .numOwners(REPLICATION_FACTOR)
            .compatibility()
            .enable();

      EmbeddedCacheManager cm = new DefaultCacheManager(gbuilder.build(), builder.build(), true);
      
      HotRodServerConfigurationBuilder hbuilder = new HotRodServerConfigurationBuilder();
      hbuilder.topologyStateTransfer(true);
      hbuilder.proxyHost(proxyhost);
      hbuilder.host(host);

      HotRodServer server = new HotRodServer();
      server.start(hbuilder.build(),cm);
      server.addCacheEventFilterConverterFactory(FilterConverterFactory.FACTORY_NAME, new FilterConverterFactory());
      
      System.out.println("LAUNCHED");
      
      try {
         synchronized (this) {
            this.wait();
         }
      } catch (InterruptedException e) {
         // ignore. 
      }
      
   }
}
