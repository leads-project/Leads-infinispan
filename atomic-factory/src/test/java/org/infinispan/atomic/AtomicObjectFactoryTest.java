package org.infinispan.atomic;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.test.AbstractCacheTest;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TransportFlags;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.testng.annotations.Test;

import java.util.*;
import java.util.concurrent.*;

/**
 * @author Pierre Sutra
 * @since 6.0
 */
@Test(groups = "functional", testName = "AtomicObjectFactoryTest")
public class AtomicObjectFactoryTest extends MultipleCacheManagersTest {

   private static int NCALLS=100;
   private static int NCACHES=2;
   private static List<Cache> caches = new ArrayList<Cache>();

   private static Log log = LogFactory.getLog(AtomicObjectFactory.class);


   @Test(enabled = true)
   public void basicUsageTest() throws  Exception{
      
      EmbeddedCacheManager cacheManager = cacheManagers.iterator().next();
      Cache cache = cacheManager.getCache();
      AtomicObjectFactory factory = new AtomicObjectFactory(cache);

      // 1 - Basic Usage
      Set<String> set = factory.getInstanceOf(HashSet.class, "set");
      set.add("smthing");
      assert set.contains("smthing");
      assert set.size()==1;

      // 2 - Persistence
      factory.disposeInstanceOf(HashSet.class, "set", true);
      set = factory.getInstanceOf(HashSet.class, "set", false, null, false);
      assert set.contains("smthing");

      // 3 - Optimistic execution
      ArrayList list = factory.getInstanceOf(ArrayList.class, "list", true);
      assert !list.contains("foo");
      assert !cache.containsKey("list");

   }

   @Test(enabled = true)
   public void basicPerformanceTest() throws Exception{

      EmbeddedCacheManager cacheManager = cacheManagers.iterator().next();
      Cache cache = cacheManager.getCache();
      AtomicObjectFactory factory = new AtomicObjectFactory(cache);

      Map map = (Map) factory.getInstanceOf(HashMap.class, "map", true);

      for(int i=0; i<NCALLS*10;i++){
         map.containsKey("1");
      }
      long start = System.currentTimeMillis();
      for(int i=0; i<NCALLS*10;i++){
         map.containsKey("1");
      }

      log.debug(System.currentTimeMillis() - start);

   }

   @Test(enabled = true)
   public void distributedPersistenceTest() throws Exception {

      Iterator<EmbeddedCacheManager> it = cacheManagers.iterator();
      EmbeddedCacheManager manager1 = it.next();
      EmbeddedCacheManager manager2 = it.next();
      Cache cache1 = manager1.getCache();
      AtomicObjectFactory factory1 = new AtomicObjectFactory(cache1);
      Cache cache2 = manager2.getCache();
      AtomicObjectFactory factory2 = new AtomicObjectFactory(cache2);

      HashSet set1, set2;
      
      // 1 - Concurrent retrieval
      set1 = factory1.getInstanceOf(HashSet.class, "persist");
      set1.add("smthing");
      set2 = factory2.getInstanceOf(HashSet.class, "persist", true, null, false);
      assert set2.contains("smthing");
      
      // 2 - Serial storing then retrieval
      set1 = factory1.getInstanceOf(HashSet.class, "persist2");
      set1.add("smthing");
      factory1.disposeInstanceOf(HashSet.class,"persist2",true);
      set2 = factory2.getInstanceOf(HashSet.class, "persist2", true, null, false);
      assert set2.contains("smthing");

   }

   @Test(enabled = true)
   public void distributedCacheTest() throws Exception {

      ExecutorService service = Executors.newCachedThreadPool();
      List<Future<Integer>> futures = new ArrayList<>();

      for(EmbeddedCacheManager manager: cacheManagers){
         Cache cache = manager.getCache();
         caches.add(cache);
         futures.add(service.submit(
               new ExerciseAtomicSetTask(
                     new AtomicObjectFactory(cache), NCALLS)));
      }

      Integer total = 0;
      for(Future<Integer> future : futures){
         total += future.get();
      }

      assert total == (NCALLS) : "obtained = "+total+"; espected = "+ (NCALLS);

   }


   //
   // HELPERS
   //

   @Override
   protected void createCacheManagers() throws Throwable {
      ConfigurationBuilder builder
            = AbstractCacheTest.getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC, true);
      builder.clustering().hash().numOwners(1);
      TransportFlags flags = new TransportFlags();
      createClusteredCaches(NCACHES, builder, flags);
   }

   protected void initAndTest() {
      for (Cache<Object, String> c : caches) assert c.isEmpty();
      caches.iterator().next().put("k1", "value");
      assertOnAllCaches("k1", "value");
   }

   protected void assertOnAllCaches(Object key, String value) {
      for (Cache<Object, String> c : caches) {
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

   //
   // UTILITY CLASSES
   //

   public static class ExerciseAtomicSetTask implements Callable<Integer>{

      private static final String name="aset";
      
      private int ncalls;
      private Set set;
      private AtomicObjectFactory factory; 

      public ExerciseAtomicSetTask(AtomicObjectFactory f, int n){
         factory = f;
         ncalls = n;
      }

      @Override
      public Integer call() throws Exception {
         int ret = 0;
         for(int i=0; i<ncalls;i++){
            if (set==null) {
               synchronized (this.getClass()) {
                  set = factory.getInstanceOf(HashSet.class, name, false, null, false);
               }
            }
            boolean r = set.add(i);
            // if successful, close then re-open the set
            if(r){
               ret ++;
               if (ret%NCACHES==0) {
                  synchronized (this.getClass()) {
                     factory.disposeInstanceOf(HashSet.class, name, true);
                     set = null;
                  }
               }
            }
         }
         return  ret;
      }
   }


}

