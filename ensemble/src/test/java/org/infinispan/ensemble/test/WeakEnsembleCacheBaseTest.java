package org.infinispan.ensemble.test;

import example.avro.WebPage;
import org.infinispan.ensemble.EnsembleCacheManager;
import org.infinispan.ensemble.cache.EnsembleCache;
import org.testng.annotations.Test;


/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */
@Test(groups = "functional", testName = "WeakEnsembleCacheBaseTest")
public class WeakEnsembleCacheBaseTest extends EnsembleCacheBaseTest {

    private EnsembleCache<CharSequence,WebPage> cache;

    @Override
    protected synchronized EnsembleCache<CharSequence, WebPage> cache() {
        if (cache==null)
            cache = manager.getCache(cacheName(),numberOfSites()/2, EnsembleCacheManager.Consistency.WEAK);
        return cache;
    }

    @Override
    protected int numberOfSites() {
        return 3;
    }
}
