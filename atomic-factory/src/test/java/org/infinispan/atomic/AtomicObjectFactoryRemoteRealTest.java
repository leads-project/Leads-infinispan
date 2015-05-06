package org.infinispan.atomic;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.api.BasicCacheContainer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Pierre Sutra
 */
public class AtomicObjectFactoryRemoteRealTest extends AtomicObjectFactoryAbstractTest {

   private static List<BasicCacheContainer> remoteCacheManagers = new ArrayList<>();


   @Override 
   public BasicCacheContainer container(int i) {
      return remoteCacheManagers.get(i%remoteCacheManagers.size());
   }

   @Override 
   public Collection<BasicCacheContainer> containers() {
      return remoteCacheManagers;
   }

   @Override 
   protected void createCacheManagers() throws Throwable {
      
      for(String server : servers()) {
         int port = Integer.valueOf(server.split(":")[1]);
         String host = server.split(":")[0];
         org.infinispan.client.hotrod.configuration.ConfigurationBuilder cb
               = new org.infinispan.client.hotrod.configuration.ConfigurationBuilder();
         cb.tcpNoDelay(true)
               .addServer()
               .host(host)
               .port(port);
         RemoteCacheManager manager= new RemoteCacheManager(cb.build());
         remoteCacheManagers.add(manager);
      }
      this.cleanup = null;
   }

   protected String[] servers () {
      return new String[]{"localhost:11222"};
   }

}
