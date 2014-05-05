package org.infinispan.versioning;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.versioning.impl.VersionedCacheFinedGrainedHashMapImpl;
import org.infinispan.versioning.utils.version.VersionGenerator;
import org.testng.annotations.Test;

/**
 * @author marcelo pasin, pierre sutra
 * @since 7.0
 */
@Test(testName = "versioning.VersionedFineGraineCacheAtomicMapTest", enabled = false)
public class VersionedCacheFineGrainedHashMapTest extends VersionedCacheAbstractTest {

    @Override
    protected void setBuilder(ConfigurationBuilder builder) {
    }

    @Override
    protected <K, V> VersionedCache<K, V> getCache(Cache cache, VersionGenerator generator, String name) {
        return new VersionedCacheFinedGrainedHashMapImpl<K, V>(cache,generator,name);
    }

}
