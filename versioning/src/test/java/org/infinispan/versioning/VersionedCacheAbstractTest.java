package org.infinispan.versioning;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TransportFlags;
import org.infinispan.versioning.utils.version.Version;
import org.infinispan.versioning.utils.version.VersionGenerator;
import org.infinispan.versioning.utils.version.VersionScalarGenerator;
import org.testng.annotations.Test;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author marcelo pasin, pierre sutra
 * @since 7.0
 */

public abstract class VersionedCacheAbstractTest extends MultipleCacheManagersTest {

    private static int NCACHES = 1;
    private static int NCALLS = 100;
    private static int NKEYS = 1000;

    private List<Cache> delegates = new ArrayList<Cache>(NCACHES);
    private List<VersionedCache> vcaches = new ArrayList<VersionedCache>(NCACHES);
    private Random rand = new Random(System.nanoTime());

    @Override
    protected void createCacheManagers() throws Throwable {
        ConfigurationBuilder builder = getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC, true);
        TransportFlags flags = new TransportFlags();
        setBuilder(builder);
        createClusteredCaches(NCACHES, builder, flags);
    }

    protected abstract void setBuilder(ConfigurationBuilder builder);

    protected abstract <K,V> VersionedCache<K, V> getCache(
            Cache cache,
            VersionGenerator generator,
            String name);

    @Test(enabled = true)
    public void basicUsageTest() throws Exception {
        VersionGenerator generator = new VersionScalarGenerator();
        Cache cache = caches().iterator().next();
        VersionedCache<String, String> vcache = getCache(cache,generator,"basictest");

        vcache.put("k", "a");
        vcache.put("k", "b");
        Version earliest = vcache.getEarliestVersion("k");
        assert 0 == vcache.get("k", earliest, earliest).size();
        Version next = generator.generateNew();
        assert 1 == vcache.get("k", earliest, next).size();
        assert vcache.get("k").equals("b");

        Map<Version,String> m = new HashMap<Version,String>();
        m.put(generator.generateNew(),"c");
        m.put(generator.generateNew(),"d");
        vcache.putAll("k", m);
        assert vcache.get("k").equals("d");

    }

    @Test(enabled = true)
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

    @Test(enabled = true)
    public void basicDistributedUsage() throws Exception {
        ExecutorService service = Executors.newCachedThreadPool();
        List<Future<Integer>> futures = new ArrayList<Future<Integer>>();

        for (int i = 0; i < NCACHES; i++) {
            Cache delegate = cacheManagers.get(i).getCache();
            delegates.add(delegate);
            VersionScalarGenerator generator = new VersionScalarGenerator();
            vcaches.add(getCache(delegate, generator, "test"));
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

    //
    // OBJECT METHODS
    //

    protected void initAndTest() {
        for (Cache<Object, String> c : delegates) assert c.isEmpty();
        delegates.iterator().next().put("k1", "version");
        assertOnAllCaches("k1", "version");
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
                Version first = versionedCache.getEarliestVersion(k);
                Version last = versionedCache.getLatestVersion(k);
                if (first!=null && last!=null)
                    versionedCache.get(k,first,last);
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