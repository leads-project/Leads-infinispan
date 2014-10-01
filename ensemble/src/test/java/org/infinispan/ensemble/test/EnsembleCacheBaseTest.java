package org.infinispan.ensemble.test;

import example.avro.WebPage;
import org.infinispan.ensemble.search.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryBuilder;
import org.infinispan.query.dsl.QueryFactory;
import org.testng.annotations.Test;

import java.util.List;

import static org.infinispan.client.hotrod.avro.AvroTestHelper.somePage;
import static org.testng.Assert.assertEquals;


/**
  *
 * @author Pierre Sutra
 * @since 6.0
 */
public abstract class EnsembleCacheBaseTest extends EnsembleCacheAbstractTest<WebPage> {

    @Override
    protected String cacheName(){
        return "test";
    }

    @Override
    protected Class<WebPage> beanClass(){
        return WebPage.class;
    }

    @Override
    protected int numberOfSites() {
        return 1;
    }

    @Test
    public void baseOperations() {
        WebPage page1 = somePage();
        cache().put(page1.getUrl(),page1);
        assert cache().containsKey(page1.getUrl());
        assert cache().get(page1.getUrl()).equals(page1);
    }

    @Test
    public void baseQuery(){
        QueryFactory qf = Search.getQueryFactory(cache());

        WebPage page1 = somePage();
        cache().put(page1.getUrl(),page1);
        WebPage page2 = somePage();
        cache().put(page2.getUrl(),page2);

        QueryBuilder qb = qf.from(WebPage.class);
        Query query = qb.build();
        List list = query.list();
        assertEquals(list.size(),2);

        qb = qf.from(WebPage.class);
        qb.having("url").eq(page1.getUrl());
        query = qb.build();
        assertEquals(query.list().get(0), page1);
    }



}
