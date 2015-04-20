package org.infinispan.ensemble.test.distributed;

import example.avro.WebPage;
import org.infinispan.ensemble.Site;
import org.infinispan.ensemble.cache.EnsembleCache;
import org.infinispan.ensemble.cache.distributed.DistributedEnsembleCache;
import org.infinispan.ensemble.cache.distributed.HashBasedPartitioner;
import org.infinispan.ensemble.cache.distributed.Partitioner;
import org.infinispan.ensemble.test.EnsembleBaseTest;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.infinispan.client.hotrod.avro.AvroTestHelper.somePage;


/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */
@Test(groups = "functional", testName = "EnsembleDistributedCacheTest")
public class EnsembleDistributedCacheTest extends EnsembleBaseTest {

   private DistributedEnsembleCache<CharSequence, WebPage> cache;
   private Partitioner<CharSequence, WebPage> partitioner;
   private boolean frontierMode = true;

   @Override
   protected synchronized EnsembleCache<CharSequence, WebPage> cache() {
      if (cache == null) {
         List<EnsembleCache<CharSequence, WebPage>> list = new ArrayList<>();
         for (Site s : manager.sites())
            list.add(s.<CharSequence, WebPage>getCache(cacheName));
         partitioner = new HashBasedPartitioner<>(list);
         cache = (DistributedEnsembleCache<CharSequence, WebPage>) manager.getCache(cacheName, list, partitioner, frontierMode);
      }
      return cache;
   }

   @Test
   @Override
   public void baseCacheOperations() {

      WebPage page1 = somePage();
      WebPage page2 = somePage();

      // get, put
      cache().put(page1.getKey(),page1);
      assert frontierMode || cache().containsKey(page1.getKey());
      assert frontierMode || cache().get(page1.getKey()).equals(page1);

      // putIfAbsent
      for(int i=0; i<1000; i++){
         page2 = somePage();
         cache().putIfAbsent(page1.getKey(), page2);
      }
      assert frontierMode || cache().get(page1.getKey()).equals(page1);

      // Frontier mode check
      WebPage page3= somePage();
      cache().put(page3.getKey(), page3);
      EnsembleCache<CharSequence, WebPage> location = partitioner.locate(page3.getKey());
      if (!frontierMode || location.equals(cache.getFrontierCache()))
         assert cache.containsKey(page3.getKey());
      else
         assert !cache.containsKey(page3.getKey());

   }

   @Test
   @Override
   public void baseQuery() {
      if (frontierMode && numberOfNodes()==1)
         super.baseQuery();
   }

   @Test
   @Override
   public void pagination(){
      if (frontierMode && numberOfNodes()==1)
         super.pagination();
   }

   @Test
   @Override
   public void update(){
      if (frontierMode && numberOfNodes()==1)
         super.update();
   }
   
   @Test
   @Override
   public void split() {
      if (frontierMode)
         super.split();
   }

}
