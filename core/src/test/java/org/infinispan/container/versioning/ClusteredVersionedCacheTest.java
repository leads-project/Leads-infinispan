package org.infinispan.container.versioning;

import org.infinispan.Cache;
import org.infinispan.VersionedCache;
import org.infinispan.VersionedCacheImpl;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.VersioningScheme;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TransportFlags;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/**
 * // TODO: Document this
 *
 * @author Pierre Sutra
 * @since 6.0
 */

@Test(testName = "container.versioning.AbstractClusteredWriteSkewTest", groups = "functional")
public class ClusteredVersionedCacheTest extends MultipleCacheManagersTest {

    private static int NCACHES = 3;
    private static int NCALLS = 10;
    private List<Cache> delegates = new ArrayList<Cache>(NCACHES);
    private List<VersionedCache> vcaches = new ArrayList<VersionedCache>(NCACHES);

    @Override
    protected void createCacheManagers() throws Throwable {
        ConfigurationBuilder builder = getDefaultClusteredCacheConfig(CacheMode.REPL_SYNC, true);
        builder.versioning().scheme(VersioningScheme.SIMPLE);
        TransportFlags flags = new TransportFlags();
        createClusteredCaches(NCACHES, builder, flags);
    }


    public void basicUsageTest() throws  Exception{
        EmbeddedCacheManager cacheManager = cacheManagers.iterator().next();
        Cache cache = cacheManager.getCache();
        NumericVersionGenerator generator = new NumericVersionGenerator();
        VersionedCache<String,String> vcache = new VersionedCacheImpl<String,String>(cache,generator,"test");
        vcache.put("k","a");
        vcache.put("k","b");
        assert vcache.size()==2;
        vcache.getLatestVersion("k").compareTo(vcache.getEarliestVersion("k")).equals(InequalVersionComparisonResult.AFTER);
        assert vcache.get("k",vcache.getEarliestVersion("k"),vcache.getEarliestVersion("k")).size()==0;
        assert vcache.get("k",generator.generateNew(),generator.generateNew()).size()==1;
    }

    public void basicDistributedUsage() throws Exception{
        ExecutorService service = Executors.newCachedThreadPool();
        List<Future<Integer>> futures = new ArrayList<Future<Integer>>();

        for(int i=0; i<NCACHES; i++){
            Cache delegate = cacheManagers.get(i).getCache();
            delegates.add(delegate);
            SimpleClusteredVersionGenerator generator = new SimpleClusteredVersionGenerator();
            generator.init(delegate);
            generator.start();
            generator.setTopologyID(i);
            vcaches.add(new VersionedCacheImpl(delegate,generator,"test"));
        }

        // simple test to create the topology.
        initAndTest();

        for(VersionedCache  vcache : vcaches){
            futures.add(service.submit(new ExerciceVersionedCache(vcache,NCALLS)));
        }

        Integer total = 0;
        for(Future<Integer> future : futures){
            total += future.get();
        }

        System.out.println(vcaches.get(0).values());
        assert total == NCACHES*NCALLS;

    }


    private class ExerciceVersionedCache implements Callable<Integer> {

        private int ncalls;
        private VersionedCache versionedCache;

        public ExerciceVersionedCache(VersionedCache<String,String> vcache, int n){
            versionedCache = vcache;
            ncalls = n;
        }

        @Override
        public Integer call() throws Exception {
            int ret = 0;
            Random rand = new Random(System.nanoTime());
            for(int i=0; i<ncalls;i++){
                versionedCache.put(Integer.toString(rand.nextInt()), Integer.toString(i));
            }
            return  new Integer(ncalls);
        }
    }

    protected void initAndTest() {
        for (Cache<Object, String> c : delegates) assert c.isEmpty();
        delegates.iterator().next().put("k1", "value");
        assertOnAllCaches("k1", "value");
    }

    protected void assertOnAllCaches(Object key, String value) {
        for (Cache<Object, String> c : delegates) {
            Object realVal = c.get(key);
            if (value == null) {
                assert realVal == null : "Expecting [" + key + "] to equal [" + value + "] on cache "+ c.toString();
            } else {
                assert value.equals(realVal) : "Expecting [" + key + "] to equal [" + value + "] on cache "+c.toString();
            }
        }
        // Allow some time for all ClusteredGetCommands to finish executing
        TestingUtil.sleepThread(1000);
    }

}
