package org.infinispan.atomic.utils;

import org.infinispan.atomic.filter.FilterConverterFactory;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.infinispan.test.AbstractCacheTest.getDefaultClusteredCacheConfig;

/**
 * @author Pierre Sutra
 */
public class Server implements Runnable {

   private static final String defaultHost ="localhost";
   private boolean USE_TRANSACTIONS = false;
   private CacheMode CACHE_MODE = CacheMode.DIST_SYNC;
      
   @Option(name = "-host", required = true, usage = "host or ip address of local machine")
   private String host = defaultHost;
   
   @Option(name = "-proxy", usage = "proxy host as seen by clients")
   private String proxyhost = defaultHost;

   @Option(name = "-rf", usage = "replication factor")
   private int replicationFactor = 1;

   @Option(name = "-p", usage = "use persistence via a single file data store (emptied at each start)")
   private boolean usePersistency = false;

   public Server () {}
   
   public static void main(String args[]) {
      new Server().doMain(args);
   }
   
   
   public void doMain(String[] args) {
      
      CmdLineParser parser = new CmdLineParser(this);

      parser.setUsageWidth(80);

      try {
         if(args.length<2)
            throw new CmdLineException(parser,"No argument is given");
         parser.parseArgument(args);
      } catch( CmdLineException e ) {
         System.err.println(e.getMessage());
         parser.printUsage(System.err);
         System.err.println();
         return;
      }

      ExecutorService executor = Executors.newSingleThreadExecutor();
      executor.execute(this);
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
            .hash()
            .numOwners(replicationFactor)
            .locking().useLockStriping(true)
            .compatibility()
            .enable();
      builder.locking()
            .concurrencyLevel(10000)
            .useLockStriping(false);

      if (usePersistency)
         builder.persistence()
               .addSingleFileStore()
               .location(System.getProperty("."))
               .purgeOnStartup(true)
               .create();

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
