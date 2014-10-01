package org.infinispan.ensemble.test;

import example.avro.WebPage;
import org.infinispan.ensemble.cache.EnsembleCache;
import org.infinispan.ensemble.search.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryBuilder;
import org.infinispan.query.dsl.QueryFactory;
import org.testng.annotations.Test;

import static org.infinispan.client.hotrod.avro.AvroTestHelper.createPage1;
import static org.infinispan.client.hotrod.avro.AvroTestHelper.createPage2;


/**
  *
 * @author Pierre Sutra
 * @since 6.0
 */
public abstract class EnsembleCacheBaseTest extends EnsembleCacheAbstractTest<WebPage> {

    protected abstract EnsembleCache<CharSequence,WebPage>cache();

    @Override
    protected String cacheName(){
        return "test";
    }

    @Override
    protected Class<WebPage> beanClass(){
        return WebPage.class;
    }

    @Test
    public void baseOperations() throws Exception {
        WebPage page1 = createPage1();
        cache().put(page1.getUrl(),page1);
        assert cache().containsKey(page1.getUrl());
        assert cache().get(page1.getUrl()).equals(page1);
    }

    @Test
    public void baseQuery(){
        QueryFactory qf = Search.getQueryFactory(cache());

        WebPage page1 = createPage1();
        cache().put(page1.getUrl(),page1);
        WebPage page2 = createPage2();
        cache().put(page2.getUrl(),page2);

        QueryBuilder qb = qf.from(WebPage.class);
        Query query = qb.build();
        assert(query.list().size()==2);

        qb = qf.from(WebPage.class);
        qb.having("url").eq(page1.getUrl());
        query = qb.build();
        assert(query.list().get(0).equals(page1));
    }



}
