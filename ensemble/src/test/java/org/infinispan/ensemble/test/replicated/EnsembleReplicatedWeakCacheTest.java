package org.infinispan.ensemble.test.replicated;

import example.avro.WebPage;
import org.infinispan.ensemble.EnsembleCacheManager;
import org.infinispan.ensemble.cache.EnsembleCache;
import org.infinispan.ensemble.test.EnsembleBaseTest;
import org.infinispan.manager.CacheContainer;
import org.testng.annotations.Test;


/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */
@Test(groups = "functional", testName = "EnsembleReplicatedWeakCacheTest")
public class EnsembleReplicatedWeakCacheTest extends EnsembleBaseTest {

    private EnsembleCache<CharSequence,WebPage> cache;

    @Override
    protected synchronized EnsembleCache<CharSequence, WebPage> cache() {
        if (cache==null)
            cache = manager.getCache(CacheContainer.DEFAULT_CACHE_NAME,numberOfSites()/2, EnsembleCacheManager.Consistency.WEAK);
        return cache;
    }

    @Override
    protected int numberOfSites() {
        return 3;
    }
}
