package org.infinispan.versioning;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.container.versioning.InequalVersionComparisonResult;
import org.infinispan.container.versioning.NumericVersionGenerator;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.fwk.TransportFlags;
import org.infinispan.versioning.impl.VersionedCacheNaiveImpl;
import org.testng.annotations.Test;

@Test(testName = "container.versioning.AbstractClusteredWriteSkewTest", groups = "functional")
  public class VersionedCacheNaiveTest  extends MultipleCacheManagersTest {
    private static int NCACHES = 1;
    @Override
      protected void createCacheManagers() throws Throwable {
      ConfigurationBuilder builder = getDefaultClusteredCacheConfig(
								    CacheMode.REPL_SYNC, true);
      TransportFlags flags = new TransportFlags();
      createClusteredCaches(NCACHES, builder, flags);
    }

    public void basicUsageTest() throws  Exception{
      EmbeddedCacheManager cacheManager = cacheManagers.iterator().next();
      Cache cache = cacheManager.getCache();
      NumericVersionGenerator generator = new NumericVersionGenerator();
      VersionedCache<String,String> vcache = new VersionedCacheNaiveImpl<String, String>(cache,generator,"test");
      vcache.put("k","a");
      vcache.put("k","b");
      assert vcache.size()==2;
      vcache.getLatestVersion("k").compareTo(vcache.getEarliestVersion("k")).equals(InequalVersionComparisonResult.AFTER);
      assert vcache.get("k",vcache.getEarliestVersion("k"),vcache.getEarliestVersion("k")).size()==0;
      assert vcache.get("k",generator.generateNew(),generator.generateNew()).size()==1;
    }
  }
