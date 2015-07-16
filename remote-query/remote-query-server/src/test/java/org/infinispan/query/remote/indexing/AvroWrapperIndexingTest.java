package org.infinispan.query.remote.indexing;

import example.avro.User;
import example.avro.WebPage;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.query.Search;
import org.infinispan.query.SearchManager;
import org.infinispan.query.remote.client.avro.AvroSupport;
import org.infinispan.test.SingleCacheManagerTest;
import org.testng.annotations.Test;
import sun.text.normalizer.UCharacter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit test for Avro based indexation.
 *
 * @author Pierre SUtra
 * @since 7.0
 */
@Test(groups = "functional", testName = "AvroWrapperIndexingTest")
public class AvroWrapperIndexingTest extends SingleCacheManagerTest {

   @Override
   protected EmbeddedCacheManager createCacheManager() throws Exception {
      ConfigurationBuilder cfg = getDefaultStandaloneCacheConfig(false);
      cfg.indexing().enable()
            .addProperty("default.directory_provider", "ram")
            .addProperty("lucene_version", "LUCENE_CURRENT");
      cfg.validate();
      
      Configuration configuration = cfg.build();
      assert cfg.clustering().cacheMode() == CacheMode.LOCAL;
      
      EmbeddedCacheManager cacheManager = new DefaultCacheManager(configuration);
      cacheManager.getCache(); //TODO this ensures the GlobalComponentRegistry is initialised right now, but it's not the cleanest way
      return cacheManager;
   }

   public void testIndexingWithWrapper() throws Exception {
      User user = new User();
      user.setName("Alice");
      user.setFavoriteNumber(12);

      org.apache.avro.Schema schema = user.getSchema();
      GenericRecord guser = new GenericData.Record(schema);
      guser.put("name",user.getName());
      guser.put("favorite_number",user.getFavoriteNumber());
      cache.put("user", guser);

      SearchManager sm = Search.getSearchManager(cache);

      SearchFactoryImplementor searchFactory = (SearchFactoryImplementor) sm.getSearchFactory();
      assertNotNull(searchFactory.getIndexManagerHolder().getIndexManager(GenericData.Record.class.getName()));

      Query luceneQuery = sm.buildQueryBuilderForClass(GenericData.Record.class)
            .get()
            .keyword()
            .onField("name")
            .ignoreFieldBridge()
            .ignoreAnalyzer()
            .matching("Alice")
            .createQuery();

      List<Object> list = sm.getQuery(luceneQuery).list();
      assertEquals(1, list.size());

      luceneQuery = sm.buildQueryBuilderForClass(GenericData.Record.class)
            .get()
            .range()
            .onField("name")
            .ignoreFieldBridge()
            .ignoreAnalyzer()
            .above("Bob")
            .createQuery();

      list = sm.getQuery(luceneQuery).list();
      assertEquals(0, list.size());

      // FIXME add support for numeric queries.
      luceneQuery = sm.buildQueryBuilderForClass(GenericData.Record.class)
            .get()
            .range()
            .onField("favorite_number")
            .ignoreFieldBridge()
            .ignoreAnalyzer()
            .above(11)
            .createQuery();

      list = sm.getQuery(luceneQuery).list();
      // assertEquals(1, list.size());

   }

   public void testIndexingWithWrapperWebPage() throws Exception {
      WebPage page = new WebPage();
      page.setKey("http://www.test.com");
      Map<CharSequence,CharSequence> outlinks = new HashMap<>();
      outlinks.put("1","http://www.example.com");
      page.setOutlinks(outlinks);

      SearchManager sm = Search.getSearchManager(cache);

      cache.put("page",page);

      SearchFactoryImplementor searchFactory = (SearchFactoryImplementor) sm.getSearchFactory();
      assertNotNull(searchFactory.getIndexManagerHolder().getIndexManager(GenericData.Record.class.getName()));

      Query luceneQuery = sm.buildQueryBuilderForClass(GenericData.Record.class)
            .get()
            .keyword()
            .onField("outlinks")
            .ignoreFieldBridge()
            .ignoreAnalyzer()
            .matching(outlinks)
            .createQuery();

      List<Object> list = list = sm.getQuery(luceneQuery).list();
      // assertEquals(1, list.size());

   }

}
