package org.infinispan.atomic;

import org.infinispan.commons.api.BasicCacheContainer;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.test.AbstractCacheTest;
import org.infinispan.test.fwk.TransportFlags;
import org.testng.annotations.Test;

import java.util.Collection;

/**
 * @author Pierre Sutra
 * @since 6.0
 */
@Test(groups = "functional", testName = "AtomicObjectFactoryTest")
public class AtomicObjectFactoryTest extends AtomicObjectFactoryAbstractTest {


   //
   // HELPERS
   //

   @Override 
   public BasicCacheContainer container(int i) {
      return manager(i);
   }

   @Override 
   public Collection<BasicCacheContainer> containers() {
      return (Collection)getCacheManagers();
   }

   @Override
   protected void createCacheManagers() throws Throwable {
      ConfigurationBuilder builder
            = AbstractCacheTest.getDefaultClusteredCacheConfig(CACHE_MODE, USE_TRANSACTIONS);
      builder.clustering().hash().numOwners(REPLICATION_FACTOR);
      TransportFlags flags = new TransportFlags();
      createClusteredCaches(NMANAGERS, builder, flags);
   }

}

