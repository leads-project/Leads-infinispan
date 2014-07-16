package org.infinispan.versioning;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.versioning.impl.VersionedCacheShardedTreeMapImpl;
import org.infinispan.versioning.utils.version.VersionGenerator;
import org.testng.annotations.Test;

/**
 *
 * @author Pierre Suttra
 * @since 4.0
 */
@Test(groups = "functional", testName = "VersionedCacheShardedTreeMapTest")
public class VersionedCacheShardedTreeMapTest extends VersionedCacheAbstractTest {

    @Override
    protected void setBuilder(ConfigurationBuilder builder) {
    }

    @Override
    protected <K, V> VersionedCache<K, V> getCache(Cache cache, VersionGenerator generator, String name) {
        return new VersionedCacheShardedTreeMapImpl<K, V>(cache,generator,name);
    }

}
