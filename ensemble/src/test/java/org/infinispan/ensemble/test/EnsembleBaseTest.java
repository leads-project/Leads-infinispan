package org.infinispan.ensemble.test;

import example.avro.WebPage;
import org.infinispan.client.hotrod.impl.avro.AvroQueryBuilder;
import org.infinispan.client.hotrod.impl.avro.AvroRemoteQuery;
import org.infinispan.commons.util.concurrent.NotifyingFuture;
import org.infinispan.ensemble.search.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryBuilder;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.dsl.SortOrder;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.infinispan.client.hotrod.avro.AvroTestHelper.somePage;
import static org.testng.Assert.assertEquals;


/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public abstract class EnsembleBaseTest extends EnsembleAbstractTest<CharSequence, WebPage> {

   public static final String cacheName = "WebPage";

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
      return 1;
   }

   @Override
   protected int numberOfNodes() {
      return 1;
   }

   @Test
   public void baseManagerOperations() {
      getManager().start();
      getManager().stop();
      getManager().start();
   }
   
   @Test
   public void baseCacheOperations() {
      WebPage page1 = somePage();
      WebPage page2 = somePage();

      // get
      cache().put(page1.getKey(),page1);
      assert cache().containsKey(page1.getKey());
      assert cache().get(page1.getKey()).equals(page1);

      // putIfAbsent
      assert cache().putIfAbsent(page2.getKey(),page2)==null;
      cache().putIfAbsent(page1.getKey(),page2);
      assert cache().get(page2.getKey()).equals(page2);

   }

   @Test
   public void asyncBaseOperations() {

      final int PAGES=100;

      List<NotifyingFuture<WebPage>> futures = new ArrayList<>();
      for (int i=0; i<PAGES; i++) {
         WebPage page = somePage();
         futures.add(
               cache().putIfAbsentAsync(page.getKey(), page));
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
      cache().put(page1.getKey(),page1);
      WebPage page2 = somePage();
      cache().put(page2.getKey(),page2);

      QueryBuilder qb = qf.from(WebPage.class);
      Query query = qb.build();
      List list = query.list();
      assertEquals(list.size(),2);

      qb = qf.from(WebPage.class);
      qb.having("key").eq(page1.getKey());
      query = qb.build();
      assertEquals(query.list().get(0), page1);

   }

   @Test
   public void pagination() {

      int NPAGES = 100;

      Map<String, WebPage> added = new HashMap<>();
      for(int i=0; i<NPAGES; i++) {
         WebPage page = somePage();
         cache().put(page.getKey(), page);
         added.put(page.getKey().toString(), page);
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
         retrieved.put(page.getKey().toString(),page);
      }

      AssertJUnit.assertEquals(added.size(), retrieved.size());

   }

   @Test
   public void update(){

      int NITERATIONS = 100;

      WebPage page1 = somePage();
      for (int i=0; i <NITERATIONS; i++){
         WebPage page = somePage();
         cache().put(page1.getKey(),page);
         WebPage page2 = cache().get(page1.getKey());
         assert  page2!=null;
         assert page2.equals(page);
      }

      QueryFactory qf = Search.getQueryFactory(cache());
      Query query = qf.from(WebPage.class).build();
      assertEquals(query.list().size(),1);
   }
   
   
   @Test
   public void split() {
      
      int NPAGES = 100;
      for (int i=0; i <NPAGES; i++){
         WebPage page = somePage();
         cache().put(page.getKey(),page);
      }
      
      QueryFactory qf = Search.getQueryFactory(cache());
      AvroQueryBuilder qb = (AvroQueryBuilder) qf.from(WebPage.class);
      Query query = qb.build();
      Collection<AvroRemoteQuery> split = qb.split(query);
      
      Collection<WebPage> results = new ArrayList<>();
      for (Query q : split) {
         results.addAll(q.<WebPage>list());
      }
      
      assertEquals(results.size(),NPAGES);

   }

   // Helpers

   @Override
   public List<String> cacheNames(){
      List<String> cacheNames = new ArrayList<>();
      cacheNames.add(cacheName);
      return cacheNames;
   }
   
}
