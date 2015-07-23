package org.infinispan.client.hotrod.avro;

import example.avro.Employee;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.impl.avro.AvroSearch;
import org.infinispan.client.hotrod.test.MultiHotRodServersTest;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.remote.client.avro.AvroMarshaller;
import org.infinispan.query.remote.client.avro.AvroSupport;
import org.infinispan.test.fwk.CleanupAfterMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.infinispan.client.hotrod.avro.AvroTestHelper.createEmployee1;
import static org.infinispan.client.hotrod.avro.AvroTestHelper.createEmployee2;
import static org.infinispan.server.hotrod.test.HotRodTestingUtil.hotRodCacheConfiguration;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 * @author otrack
 * @since 4.0
 */
@Test(testName = "client.hotrod.avro.MultiHotRodAvroQueryTest", groups = "functional")
@CleanupAfterMethod
public class MultiHotRodAvroQueryTest extends MultiHotRodServersTest {

   protected RemoteCache<Integer, Employee> remoteCache0, remoteCache1;

   @Override
   protected void createCacheManagers() throws Throwable {
      ConfigurationBuilder builder = hotRodCacheConfiguration(getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC, false));
      builder.indexing().enable()
            .addProperty("default.directory_provider", "ram")
            .addProperty("lucene_version", "LUCENE_CURRENT")
            .clustering().hash().numOwners(1);

      createHotRodServers(2, builder);

      remoteCache0 = client(0).getCache();
      remoteCache1 = client(1).getCache();

      AvroSupport.registerSchema(client(0), Employee.getClassSchema());
      AvroSupport.registerSchema(client(1), Employee.getClassSchema());

   }

   @Override
   protected RemoteCacheManager createClient(int i) {
      org.infinispan.client.hotrod.configuration.ConfigurationBuilder clientBuilder
            = new org.infinispan.client.hotrod.configuration.ConfigurationBuilder();
      clientBuilder.addServer().host(server(i).getHost()).port(server(i).getPort());
      clientBuilder.marshaller(new AvroMarshaller<Employee>(Employee.class));
      return new RemoteCacheManager(clientBuilder.build());
   }

   @Test
   public void testEmbeddedAttributeQuery() throws Exception {
      remoteCache0.put(1, createEmployee1());
      remoteCache0.put(2, createEmployee2());

      QueryFactory qf = AvroSearch.getQueryFactory(remoteCache0);

      Query query = qf.from(Employee.class)
            .having("ssn").lte("12357").toBuilder()
            .build();
      List<Employee> list = query.list();
      assertNotNull(list);
      assertEquals(1, list.size());

   }

   @Override
   protected org.infinispan.client.hotrod.configuration.ConfigurationBuilder createHotRodClientConfigurationBuilder(int serverPort) {
      org.infinispan.client.hotrod.configuration.ConfigurationBuilder clientBuilder = new org.infinispan.client.hotrod.configuration.ConfigurationBuilder();
      clientBuilder.addServer()
            .host("localhost")
            .port(serverPort)
            .pingOnStartup(false);
      clientBuilder.marshaller(new AvroMarshaller<>(Employee.class));
      return clientBuilder;
   }


}
