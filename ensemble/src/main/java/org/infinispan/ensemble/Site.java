package org.infinispan.ensemble;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.infinispan.ensemble.cache.SiteEnsembleCache;
import org.infinispan.ensemble.indexing.Indexable;
import org.infinispan.manager.CacheContainer;
import org.infinispan.util.KeyValuePair;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * A site is a geographical location where an ISPN instance is deployed.
 * This deployment is accessed via the container field of the site.
 * A single site can be marked local.
 *
 * @author Pierre Sutra, Marcelo Pasin
 * @since 6.0
 */

public class Site extends Indexable {


   //
   // CLASS FIELDS
   //
   private transient static final Log log = LogFactory.getLog(Site.class);
   private transient static Site localSite;

   //
   // OBJECT FIELDS
   //

   private String name;
   private transient boolean isLocal;
   private transient RemoteCacheManager container;

   public static Site valueOf(String servers, ConfigurationBuilder configurationBuilder, boolean isLocal){
      
      // we shuffle as HotRod provides only RoundRobin balancing strategy

      List<KeyValuePair<String,Integer>> serverList = new ArrayList<>();
      for(String server : servers.split(",")) {
         String host;
         int port;
         if (server.contains(":")) {
            host = server.split(":")[0];
            port = Integer.valueOf(server.split(":")[1]);
         } else {
            host = server;
            port = ConfigurationProperties.DEFAULT_HOTROD_PORT;
         }
         serverList.add(new KeyValuePair<>(host,port));
      }

      Collections.shuffle(serverList);

      for(KeyValuePair<String,Integer> server : serverList) {
         String host = server.getKey();
         int port = server.getValue();
         configurationBuilder.addServer().host(host).port(port).pingOnStartup(false);
      }

      return new Site(servers,new RemoteCacheManager(configurationBuilder.build(),true),isLocal);
   }

   //
   // OBJECT METHODS
   //

   public Site(String name, RemoteCacheManager container, boolean isLocal) {
      this.name = name;
      this.isLocal = isLocal;
      this.container= container;
      synchronized(this.getClass()){
         if(isLocal && localSite==null)
            localSite = this;
      }
   }

   public Site(URL url, boolean isLocal) {
      name = url.toString();
      container = new RemoteCacheManager(url.getHost(), url.getPort(), true);
      this.isLocal = isLocal;
   }

   public Site(String name, URL url, boolean isLocal) {
      this.name = name;
      container = new RemoteCacheManager(url.getHost(), url.getPort(), true);
      this.isLocal = isLocal;
   }


   public boolean isLocal(){
      return isLocal;
   }

   public String getName(){
      return name;
   }

   public RemoteCacheManager getManager(){
      return container;
   }

   public <K,V> SiteEnsembleCache<K,V> getCache(String name){
      return new SiteEnsembleCache<>(this,container.getCache(name));
   }

   public <K,V> SiteEnsembleCache<K,V> getCache(){
      return getCache(CacheContainer.DEFAULT_CACHE_NAME);
   }

   public boolean isOwner(RemoteCache remoteCache){
      return container.equals(remoteCache.getRemoteCacheManager());
   }

   @Override
   public boolean equals(Object o){
      if (!(o instanceof Site)) return false;
      return ((Site)o).getName().equals(this.getName());
   }


   @Override
   public String toString(){
      return "@"+name;
   }

}
