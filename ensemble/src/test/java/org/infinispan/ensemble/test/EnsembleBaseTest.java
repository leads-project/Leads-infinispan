package org.infinispan.ensemble.test;

import example.avro.WebPage;
import org.infinispan.commons.util.concurrent.NotifyingFuture;
import org.infinispan.ensemble.search.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryBuilder;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.dsl.SortOrder;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.infinispan.client.hotrod.avro.AvroTestHelper.somePage;
import static org.testng.Assert.assertEquals;


/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public abstract class EnsembleBaseTest extends EnsembleAbstractTest<CharSequence, WebPage> {

   public static final String cacheName = "testCache";

   @Override
   protected Class<WebPage> valueClass(){
      return WebPage.class;
   }

   @Override
   protected Class<CharSequence> keyClass(){
      return CharSequence.class;
   }

   @Override
   protected int numberOfSites() {
      return 2;
   }

   @Override
   protected int numberOfNodes() {
      return 2;
   }

   @Test
   public void baseOperations() {
      WebPage page1 = somePage();
      WebPage page2 = somePage();

      // get
      cache().put(page1.getUrl(),page1);
      assert cache().containsKey(page1.getUrl());
      assert cache().get(page1.getUrl()).equals(page1);

      // putIfAbsent
      assert cache().putIfAbsent(page2.getUrl(),page2)==null;
      cache().putIfAbsent(page1.getUrl(),page2);
      assert cache().get(page2.getUrl()).equals(page2);

   }

   @Test
   public void asyncBaseOperations() {

      final int PAGES=100;

      List<NotifyingFuture<WebPage>> futures = new ArrayList<>();
      for (int i=0; i<PAGES; i++) {
         WebPage page = somePage();
         futures.add(
               cache().putIfAbsentAsync(page.getUrl(), page));
      }

      for (NotifyingFuture<WebPage> future : futures) {
         try {
            AssertJUnit.assertEquals(null, future.get());
         } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
         }
      }

      AssertJUnit.assertEquals(PAGES,cache().size());
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

   @Test
   public void pagination() {

      int NPAGES = 100;

      Map<String, WebPage> added = new HashMap<>();
      for(int i=0; i<NPAGES; i++) {
         WebPage page = somePage();
         cache().put(page.getUrl(), page);
         added.put(page.getUrl().toString(), page);
      }

      QueryFactory qf = Search.getQueryFactory(cache());

      Map<String, WebPage> retrieved = new HashMap<>();
      for (int i=0; i< NPAGES; i++) {
         Query query = qf.from(WebPage.class)
               .maxResults(1)
               .startOffset(i)
               .orderBy("url", SortOrder.ASC)
               .build();
         assertEquals(query.list().size(),1);
         WebPage page = (WebPage) query.list().get(0);
         retrieved.put(page.getUrl().toString(),page);
      }

      AssertJUnit.assertEquals(added.size(), retrieved.size());

   }

   @Test
   public void update(){

      int NITERATIONS = 100;

      WebPage page1 = somePage();
      for (int i=0; i <NITERATIONS; i++){
         WebPage page = somePage();
         cache().put(page1.getUrl(),page);
         WebPage page2 = cache().get(page1.getUrl());
         assert  page2!=null;
         assert page2.equals(page);
      }

      QueryFactory qf = Search.getQueryFactory(cache());
      Query query = qf.from(WebPage.class).build();
      assertEquals(query.list().size(),1);
   }

   @Override
   public List<String> cacheNames(){
      List<String> cacheNames = new ArrayList<>();
      cacheNames.add(cacheName);
      return cacheNames;
   }

}
