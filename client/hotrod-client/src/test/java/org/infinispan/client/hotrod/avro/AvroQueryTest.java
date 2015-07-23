package org.infinispan.client.hotrod.avro;

import example.avro.Employee;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.TestHelper;
import org.infinispan.client.hotrod.impl.avro.AvroSearch;
import org.infinispan.commons.equivalence.ByteArrayEquivalence;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.remote.client.avro.AvroMarshaller;
import org.infinispan.query.remote.client.avro.AvroSupport;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.test.SingleCacheManagerTest;
import org.infinispan.test.fwk.CleanupAfterMethod;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import java.util.List;

import static org.infinispan.client.hotrod.avro.AvroTestHelper.*;
import static org.infinispan.client.hotrod.test.HotRodClientTestingUtil.killRemoteCacheManager;
import static org.infinispan.client.hotrod.test.HotRodClientTestingUtil.killServers;
import static org.infinispan.server.hotrod.test.HotRodTestingUtil.hotRodCacheConfiguration;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 *
 * @author Pierre Sutra
 * @since 7.0
 */
@Test(testName = "client.hotrod.avro.AvroQueryTest", groups = "functional")
@CleanupAfterMethod
public class AvroQueryTest extends SingleCacheManagerTest {

   public static final String TEST_CACHE_NAME = "TestCache";

   private HotRodServer hotRodServer;
   private RemoteCacheManager remoteCacheManager;

   private RemoteCache<Integer, Employee> employeeCache;
   private QueryFactory employeeQF;

   // Configuration

   @Override
   protected EmbeddedCacheManager createCacheManager() throws Exception {
      if (cacheManager != null)
         return cacheManager;

      GlobalConfigurationBuilder gcb = new GlobalConfigurationBuilder().clusteredDefault();
      ConfigurationBuilder builder = hotRodCacheConfiguration(getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC, false));
      builder.indexing().enable()
            .addProperty("default.directory_provider", "ram")
            .addProperty("lucene_version", "LUCENE_CURRENT");
      builder.jmxStatistics().enable();

      cacheManager = TestCacheManagerFactory.createClusteredCacheManager(gcb, new ConfigurationBuilder());
      cacheManager.defineConfiguration(TEST_CACHE_NAME, builder.build());
      cache = cacheManager.getCache(TEST_CACHE_NAME);

      hotRodServer = TestHelper.startHotRodServer(cacheManager);

      org.infinispan.client.hotrod.configuration.ConfigurationBuilder clientBuilder =
            new org.infinispan.client.hotrod.configuration.ConfigurationBuilder();
      clientBuilder.addServer().host(hotRodServer.getHost()).port(hotRodServer.getPort());
      clientBuilder.marshaller(new AvroMarshaller<Employee>(Employee.class));
      remoteCacheManager = new RemoteCacheManager(clientBuilder.build());
      employeeCache = remoteCacheManager.getCache(TEST_CACHE_NAME);
      employeeQF = AvroSearch.getQueryFactory(employeeCache);
      AvroSupport.registerSchema(remoteCacheManager,Employee.getClassSchema());

      return cacheManager;
   }

   protected ConfigurationBuilder getConfigurationBuilder() {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.dataContainer()
            .keyEquivalence(ByteArrayEquivalence.INSTANCE);
      return builder;
   }

   // Tests

   @Test
   public void etestAttributeQuery() throws Exception {
      employeeCache.put(1, createEmployee1());
      employeeCache.put(2, createEmployee2());

      // get Employee back from remote cache and check its attributes
      Employee fromCache = employeeCache.get(1);
      assertEmployee(fromCache);

      Query query = employeeQF.from(Employee.class)
            .having("name").eq("Tom").toBuilder()
            .build();
      List<Employee> list = query.list();
      assertNotNull(list);
      assertEquals(1, list.size());
      assertEquals(Employee.class, list.get(0).getClass());
      assertEmployee(list.get(0));

      query = employeeQF.from(Employee.class)
            .having("salary").lte(6000).toBuilder()
            .build();
      list = query.list();
      assertNotNull(list);
      assertEquals(1, list.size());
      assertEquals(Employee.class, list.get(0).getClass());
      assertEmployee2(list.get(0));

      query = employeeQF.from(Employee.class)
            .having("salary").lte(10000).toBuilder()
            .build();
      list = query.list();
      assertNotNull(list);
      assertEquals(2, list.size());

      query = employeeQF.from(Employee.class)
            .having("salary").gte(5000).toBuilder()
            .build();
      list = query.list();
      assertNotNull(list);
      assertEquals(2, list.size());

   }

   @Test
   public void testProjections() throws Exception {
      employeeCache.put(1, createEmployee1());
      employeeCache.put(2, createEmployee2());

      // get Employee back from remote cache and check its attributes
      Employee fromCache = employeeCache.get(1);
      assertEmployee(fromCache);

      // get Employee back from remote cache via query and check its attributes
      Query query = employeeQF.from(Employee.class)
            .setProjection("name")
            .having("name").eq("Tom").toBuilder()
            .build();

      List<Employee> list = query.list();
      assertNotNull(list);
      assertEquals(1, list.size());
      assertEquals("Tom", list.get(0).getName());
   }

   @AfterTest
   public void release() {
      killRemoteCacheManager(remoteCacheManager);
      killServers(hotRodServer);
   }

}

