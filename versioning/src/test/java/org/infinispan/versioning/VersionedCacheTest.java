package org.infinispan.versioning;

import org.hibernate.search.cfg.SearchMapping;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TransportFlags;
import org.infinispan.versioning.impl.VersionedCacheAtomicShardedTreeMapImpl;
import org.infinispan.versioning.impl.VersionedCacheAtomicTreeMapImpl;
import org.infinispan.versioning.utils.hibernate.HibernateProxy;
import org.infinispan.versioning.utils.version.VersionScalarGenerator;
import org.testng.annotations.Test;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/**
 * @author Pierre Sutra
 * @since 6.0
 */

@Test(testName = "container.versioning.AbstractClusteredWriteSkewTest", groups = "functional")
public class VersionedCacheTest extends MultipleCacheManagersTest {

    private static int NCACHES = 3;
    private static int NCALLS = 100;
    private static int NKEYS = 1000;

    private List<Cache> delegates = new ArrayList<Cache>(NCACHES);
    private List<VersionedCache> vcaches = new ArrayList<VersionedCache>(NCACHES);
    private Random rand = new Random(System.nanoTime());

    @Override
    protected void createCacheManagers() throws Throwable {
        ConfigurationBuilder builder = getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC, true);
        builder.persistence().passivation(true);
        SearchMapping mapping = new SearchMapping();
        mapping.entity(HibernateProxy.class).indexed().providedId()
                .property("k", ElementType.METHOD).field()
                .property("v", ElementType.METHOD).field()
                .property("version", ElementType.METHOD).field();

        Properties properties = new Properties();
        properties.put(org.hibernate.search.Environment.MODEL_MAPPING, mapping);
        properties.put("default.directory_provider", "ram");
        builder.indexing()
                .enable()
                .indexLocalOnly(true)
                .withProperties(properties);
        TransportFlags flags = new TransportFlags();
        createClusteredCaches(NCACHES, builder, flags);
    }


    public void basicUsageTest() throws Exception {
        EmbeddedCacheManager cacheManager = cacheManagers.iterator().next();
        Cache cache = cacheManager.getCache();
        System.out.println(cache.getCacheConfiguration().transaction().toString());
        VersionScalarGenerator generator = new VersionScalarGenerator();
        VersionedCache<String, String> vcache = new VersionedCacheAtomicShardedTreeMapImpl<String, String>(cache,generator,"cache");
        vcache.put("k", "a");
        vcache.put("k", "b");
        assert vcache.size() == 2;
        assert vcache.getLatestVersion("k").compareTo(vcache.getEarliestVersion("k"))>0;
        assert vcache.get("k", vcache.getEarliestVersion("k"), vcache.getEarliestVersion("k")).size() == 0;
        assert vcache.get("k", generator.generateNew(), generator.generateNew()).size() == 1;
    }

    public void basicDistributedUsage() throws Exception {
        ExecutorService service = Executors.newCachedThreadPool();
        List<Future<Integer>> futures = new ArrayList<Future<Integer>>();

        for (int i = 0; i < NCACHES; i++) {
            Cache delegate = cacheManagers.get(i).getCache();
            delegates.add(delegate);
            VersionScalarGenerator generator = new VersionScalarGenerator();
            vcaches.add(new VersionedCacheAtomicTreeMapImpl(delegate, generator, "test"));
        }

        initAndTest();

        for (VersionedCache vcache : vcaches) {
            if (vcache.equals(vcaches.get(0)))
                futures.add(service.submit(new ExerciceVersionedCache(vcache, NCALLS)));
        }

        Integer total = 0;
        for (Future<Integer> future : futures) {
            total += future.get();
        }

        // assert total == NCACHES*NCALLS;
        Thread.sleep(3000);

    }


    public void computeBaseLine() {
        Cache cache = cacheManagers.get(0).getCache("baseline");
        float avrg = 0;
        for (int i = 0; i < NCALLS; i++) {
            long start = System.nanoTime();
            String k = Integer.toString(rand.nextInt(NKEYS));
            cache.put(k, "a");
            avrg += System.nanoTime() - start;
        }
        System.out.println("baseline put(): " + (avrg / NCALLS) / 1000000 + " ms");
        avrg = 0;
        for (int i = 0; i < NCALLS; i++) {
            long start = System.nanoTime();
            cache.get("a");
            avrg += System.nanoTime() - start;
        }
        System.out.println("baseline get(): " + (avrg / NCALLS) / 1000000 + " ms");
    }

    //
    // OBJECT METHODS
    //

    protected void initAndTest() {
        for (Cache<Object, String> c : delegates) assert c.isEmpty();
        delegates.iterator().next().put("k1", "value");
        assertOnAllCaches("k1", "value");
    }

    protected void assertOnAllCaches(Object key, String value) {
        for (Cache<Object, String> c : delegates) {
            Object realVal = c.get(key);
            if (value == null) {
                assert realVal == null : "Expecting [" + key + "] to equal [" + value + "] on cache " + c.toString();
            } else {
                assert value.equals(realVal) : "Expecting [" + key + "] to equal [" + value + "] on cache " + c.toString();
            }
        }
        // Allow some time for all ClusteredGetCommands to finish executing
        TestingUtil.sleepThread(1000);
    }

    //
    // INNER CLASSES
    //

    private class ExerciceVersionedCache implements Callable<Integer> {

        private int ncalls;
        private VersionedCache versionedCache;

        public ExerciceVersionedCache(VersionedCache<String, String> vcache, int n) {
            versionedCache = vcache;
            ncalls = n;
        }

        @Override
        public Integer call() throws Exception {
            int ret = 0;
            float avrg = 0;
            for (int i = 0; i < ncalls; i++) {
                long start = System.nanoTime();
                String k = Integer.toString(rand.nextInt(NKEYS));
                versionedCache.put(k, Integer.toString(i));
                avrg += System.nanoTime() - start;
            }
            System.out.println("avrg put() time: " + (avrg / NCALLS) / 1000000 + " ms");

            avrg = 0;
            for (int i = 0; i < ncalls; i++) {
                String k = Integer.toString(rand.nextInt(NKEYS));
                long start = System.nanoTime();
                versionedCache.get(k);
                avrg += System.nanoTime() - start;
            }
            System.out.println("avrg get() time: " + (avrg / NCALLS) / 1000000 + " ms");

            avrg = 0;
            for (int i = 0; i < ncalls; i++) {
                String k = Integer.toString(rand.nextInt(NKEYS));
                long start = System.nanoTime();
                versionedCache.get(k, versionedCache.getEarliestVersion(k), versionedCache.getLatestVersion(k));
                avrg += System.nanoTime() - start;
            }
            System.out.println("avrg get(earliestVersion,latestVersion) time: " + (avrg / NCALLS) / 1000000 + " ms");


            for(Cache cache: caches()){
                System.out.println("Cache "+cache.getName()+" size : "+cache.size());
            }

            System.out.println();

            return new Integer(ncalls);
        }
    }

}
