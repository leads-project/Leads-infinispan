package org.infinispan.ensemble.test;

import example.avro.WebPage;
import org.infinispan.ensemble.Site;
import org.infinispan.ensemble.cache.EnsembleCache;
import org.infinispan.ensemble.cache.distributed.HashBasedPartitioner;
import org.infinispan.ensemble.cache.distributed.Partitioner;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;


/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */
@Test(groups = "functional", testName = "SWMRBaseTest")
public class DistributedEnsembleCacheBaseTest extends EnsembleCacheBaseTest {

    private EnsembleCache<CharSequence,WebPage> cache;

    @Override
    protected synchronized EnsembleCache<CharSequence, WebPage> cache() {
        if (cache==null) {
            List<EnsembleCache<CharSequence,WebPage>> list = new ArrayList<>();
            for(Site  s: manager.sites())
                list.add(s.<CharSequence, WebPage>getCache(cacheName()));
            Partitioner<CharSequence,WebPage> partitioner = new HashBasedPartitioner<>(list);
            cache = manager.getCache("cache",list,partitioner,true);
        }
        return cache;
    }

    @Override
    protected int numberOfSites() {
        return 3;
    }
}
