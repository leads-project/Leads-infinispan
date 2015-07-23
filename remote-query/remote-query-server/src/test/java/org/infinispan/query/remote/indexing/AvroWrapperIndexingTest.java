package org.infinispan.query.remote.indexing;

import example.avro.DeviceList;
import example.avro.User;
import example.avro.WebPage;
import org.apache.avro.generic.GenericData;
import org.apache.avro.specific.SpecificRecord;
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
import org.infinispan.query.remote.client.avro.AvroMarshaller;
import org.infinispan.test.SingleCacheManagerTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
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

   private static Map<Class,AvroMarshaller> marshaller = new HashMap<Class,AvroMarshaller>(){
      {
         put(User.class,new AvroMarshaller(User.class));
         put(DeviceList.class,new AvroMarshaller(DeviceList.class));
      }
   };

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

   @Test
   public void testIndexingWithWrapper() throws Exception {
      User user = new User();
      user.setName("Alice");
      user.setFavoriteNumber(12);
      addToCache(user);

      final HashMap<String,String> hardDrives
            = new HashMap<String, String>() {{put("STCD00502", "SEAGATE");put("STA045M", "SEAGATE");}};
      DeviceList deviceList = DeviceList.newBuilder().build();
      deviceList.setDevices(new ArrayList<Map<String, String>>() {{add(hardDrives);}});
      addToCache(deviceList);

      SearchManager sm = Search.getSearchManager(cache);

      SearchFactoryImplementor searchFactory = (SearchFactoryImplementor) sm.getSearchFactory();
      assertNotNull(searchFactory.getIndexManagerHolder().getIndexManager(GenericData.Record.class.getName()));

      Query luceneQuery = sm.buildQueryBuilderForClass(GenericData.Record.class)
            .get()
            .keyword()
            .onField("User.name")
            .ignoreFieldBridge()
            .ignoreAnalyzer()
            .matching("Alice")
            .createQuery();

      List<Object> list = sm.getQuery(luceneQuery).list();
      assertEquals(1, list.size());

      luceneQuery = sm.buildQueryBuilderForClass(GenericData.Record.class)
            .get()
            .range()
            .onField("User.name")
            .ignoreFieldBridge()
            .ignoreAnalyzer()
            .above("Bob")
            .createQuery();

      list = sm.getQuery(luceneQuery).list();
      assertEquals(0, list.size());

      luceneQuery = NumericRangeQuery.newIntRange("User.favorite_number", 8, 0, 12, true, true);
      list = sm.getQuery(luceneQuery).list();
      assertEquals(1, list.size());

      luceneQuery = sm.buildQueryBuilderForClass(GenericData.Record.class)
            .get()
            .keyword()
            .onField("DeviceList.devices.0")
            .ignoreFieldBridge()
            .ignoreAnalyzer()
            .matching(hardDrives)
            .createQuery();

      list = sm.getQuery(luceneQuery).list();
      list.get(0).equals(deviceList);

   }

   @Test
   public void testIndexingWithWrapperWebPage() throws Exception {
      WebPage page = new WebPage();
      page.setKey("http://www.test.com");
      Map<String,String> outlinks = new HashMap<>();
      outlinks.put("1","http://www.example.com");
      page.setOutlinks(outlinks);

      SearchManager sm = Search.getSearchManager(cache);

      cache.put("page",page);

      SearchFactoryImplementor searchFactory = (SearchFactoryImplementor) sm.getSearchFactory();
      assertNotNull(searchFactory.getIndexManagerHolder().getIndexManager(GenericData.Record.class.getName()));

      Query luceneQuery = sm.buildQueryBuilderForClass(GenericData.Record.class)
            .get()
            .keyword()
            .onField("WebPage.outlinks")
            .ignoreFieldBridge()
            .ignoreAnalyzer()
            .matching(outlinks)
            .createQuery();

      List<Object> list = list = sm.getQuery(luceneQuery).list();
      // assertEquals(1, list.size());

   }

   private void addToCache(SpecificRecord record) {
      try {
         cache.put(
               record.get(0),
               marshaller.get(record.getClass()).objectFromByteBuffer(
                     marshaller.get(record.getClass()).objectToByteBuffer(record)));
      } catch (IOException | ClassNotFoundException e) {
         e.printStackTrace();  // TODO: Customise this generated block
      }
   }

}
