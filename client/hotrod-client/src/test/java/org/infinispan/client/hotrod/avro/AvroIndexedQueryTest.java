package org.infinispan.client.hotrod.avro;

import example.avro.Employee;
import org.apache.avro.util.Utf8;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.TestHelper;
import org.infinispan.commons.equivalence.ByteArrayEquivalence;
import org.infinispan.commons.util.Util;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.remote.client.avro.AvroMarshaller;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.test.SingleCacheManagerTest;
import org.infinispan.test.fwk.CleanupAfterMethod;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.infinispan.client.hotrod.test.HotRodClientTestingUtil.killRemoteCacheManager;
import static org.infinispan.client.hotrod.test.HotRodClientTestingUtil.killServers;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * // TODO: Document this
 *
 * @author otrack
 * @since 4.0
 */
@Test(testName = "client.hotrod.avro.AvroIndexedQueryTest", groups = "functional")
@CleanupAfterMethod
public class AvroIndexedQueryTest extends SingleCacheManagerTest {

    public static final String TEST_CACHE_NAME = "EmployeeCache";

    private HotRodServer hotRodServer;
    private RemoteCacheManager remoteCacheManager;
    private RemoteCache<Integer, Employee> remoteCache;

    @Override
    protected EmbeddedCacheManager createCacheManager() throws Exception {
        GlobalConfigurationBuilder gcb = new GlobalConfigurationBuilder().nonClusteredDefault();
        ConfigurationBuilder builder = getConfigurationBuilder();
        builder.indexing().enable()
                .addProperty("default.directory_provider", "ram")
                .addProperty("lucene_version", "LUCENE_CURRENT");
        cacheManager = TestCacheManagerFactory.createCacheManager(gcb, new ConfigurationBuilder(), true);
        cacheManager.defineConfiguration(TEST_CACHE_NAME, builder.build());
        cache = cacheManager.getCache(TEST_CACHE_NAME);

        hotRodServer = TestHelper.startHotRodServer(cacheManager);

        org.infinispan.client.hotrod.configuration.ConfigurationBuilder clientBuilder = new org.infinispan.client.hotrod.configuration.ConfigurationBuilder();
        clientBuilder.addServer().host("127.0.0.1").port(hotRodServer.getPort());
        clientBuilder.marshaller(new AvroMarshaller<Employee>(Employee.class));
        remoteCacheManager = new RemoteCacheManager(clientBuilder.build());
        remoteCache = remoteCacheManager.getCache(TEST_CACHE_NAME);
        return cacheManager;
    }

    protected ConfigurationBuilder getConfigurationBuilder() {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.dataContainer()
                .keyEquivalence(ByteArrayEquivalence.INSTANCE);
        return builder;
    }

    private byte[] readClasspathResource(String classPathResource) throws IOException {
        InputStream is = getClass().getResourceAsStream(classPathResource);
        try {
            return Util.readStream(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    @AfterTest
    public void release() {
        killRemoteCacheManager(remoteCacheManager);
        killServers(hotRodServer);
    }

    public void testAttributeQuery() throws Exception {
        remoteCache.put(1, createEmployee1());
        remoteCache.put(2, createEmployee2());

        // get Employee back from remote cache and check its attributes
        Employee fromCache = remoteCache.get(1);
        assertEmployee(fromCache);

        // get Employee back from remote cache via query and check its attributes
        QueryFactory qf = Search.getQueryFactory(remoteCache);
        Query query = qf.from(Employee.class)
                .having("name").eq("Tom").toBuilder()
                .build();
        List<Employee> list = query.list();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(Employee.class, list.get(0).getClass());
        assertEmployee(list.get(0));
    }

    public void testEmbeddedAttributeQuery() throws Exception {
        remoteCache.put(1, createEmployee1());
        remoteCache.put(2, createEmployee2());

        // get Employee back from remote cache via query and check its attributes
        QueryFactory qf = Search.getQueryFactory(remoteCache);
        Query query = qf.from(Employee.class)
                .having("favoriteColor").eq("Red").toBuilder()
                .build();
        List<Employee> list = query.list();
        assertNotNull(list);
        assertEquals(0, list.size());
    }

    public void testProjections() throws Exception {
        remoteCache.put(1, createEmployee1());
        remoteCache.put(2, createEmployee2());

        // get Employee back from remote cache and check its attributes
        Employee fromCache = remoteCache.get(1);
        assertEmployee(fromCache);

        // get Employee back from remote cache via query and check its attributes
        QueryFactory qf = Search.getQueryFactory(remoteCache);
        Query query = qf.from(Employee.class)
                .setProjection("name")
                .having("name").eq("Tom").toBuilder()
                .build();

        List<Employee> list = query.list();
        assertNotNull(list);
        assertEquals(1, list.size());

        Employee Employee = list.get(0);
        assertEquals(new Utf8("Tom"), list.get(0).getName());
    }

    private Employee createEmployee1() {
        Employee Employee = new Employee();
        Employee.setName("Tom");
        Employee.setSsn("dazd");
        Employee.setDateOfBirth((long) 1);
        return Employee;
    }

    private Employee createEmployee2() {
        Employee Employee = new Employee();
        Employee.setName("Adrian");
        Employee.setSsn("eadzzad");
        Employee.setDateOfBirth((long) 2);
        return Employee;
    }

    private void assertEmployee(Employee Employee) {
        assertNotNull(Employee);
        assertEquals("Tom", Employee.getName().toString());
    }
}

