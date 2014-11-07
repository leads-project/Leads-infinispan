package org.infinispan.ensemble.test.distributed;

import example.avro.WebPage;
import org.infinispan.ensemble.Site;
import org.infinispan.ensemble.cache.EnsembleCache;
import org.infinispan.ensemble.cache.distributed.DistributedEnsembleCache;
import org.infinispan.ensemble.cache.distributed.HashBasedPartitioner;
import org.infinispan.ensemble.cache.distributed.Partitioner;
import org.infinispan.ensemble.search.Search;
import org.infinispan.ensemble.test.EnsembleBaseTest;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryBuilder;
import org.infinispan.query.dsl.QueryFactory;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.infinispan.client.hotrod.avro.AvroTestHelper.somePage;
import static org.testng.AssertJUnit.assertEquals;


/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */
@Test(groups = "functional", testName = "EnsembleDistributedCacheTest")
public class EnsembleDistributedCacheTest extends EnsembleBaseTest {

    private DistributedEnsembleCache<CharSequence, WebPage> cache;
    private Partitioner<CharSequence, WebPage> partitioner;
    private boolean frontierMode = true;

    @Override
    protected synchronized EnsembleCache<CharSequence, WebPage> cache() {
        if (cache == null) {
            List<EnsembleCache<CharSequence, WebPage>> list = new ArrayList<>();
            for (Site s : manager.sites())
                list.add(s.<CharSequence, WebPage>getCache(cacheName));
            partitioner = new HashBasedPartitioner<>(list);
            cache = (DistributedEnsembleCache<CharSequence, WebPage>) manager.getCache(cacheName, list, partitioner, frontierMode);
        }
        return cache;
    }

    @org.testng.annotations.Test
    @Override
    public void baseOperations() {
        WebPage page1 = somePage();
        cache().put(page1.getUrl(), page1);
        EnsembleCache<CharSequence, WebPage> location = partitioner.locate(page1.getUrl());
        if (!frontierMode || location.equals(cache.getFrontierCache()))
            assert cache.containsKey(page1.getUrl());
        else
            assert !cache.containsKey(page1.getUrl());
    }

    @org.testng.annotations.Test
    @Override
    public void baseQuery() {
        QueryFactory qf = Search.getQueryFactory(cache());
        QueryBuilder qb = qf.from(WebPage.class);
        Query query = qb.build();

        WebPage page1 = somePage();
        cache().put(page1.getUrl(), page1);

        EnsembleCache<CharSequence, WebPage> location = partitioner.locate(page1.getUrl());
        if (!frontierMode || location.equals(cache.getFrontierCache()))
            assertEquals(query.list().size(), 1);
        else
            assertEquals(query.list().size(), 0);

    }

}
