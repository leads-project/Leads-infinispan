package org.infinispan.atomic;

import org.infinispan.Cache;
import org.testng.annotations.Test;

import java.util.Map;

import static org.infinispan.atomic.AtomicMapLookup.getAtomicMap;
import static org.infinispan.atomic.AtomicMapLookup.getFineGrainedAtomicMap;

/**
 * @author Vladimir Blagojevic (C) 2011 Red Hat Inc.
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 * @author Pedro Ruivo
 */
@Test(groups = "functional", testName = "atomic.FineGrainedAtomicMapAPITest")
public class FineGrainedAtomicMapAPITest extends BaseAtomicHashMapAPITest {

   @SuppressWarnings("UnusedDeclaration")
   @Test(expectedExceptions = {IllegalArgumentException.class})
   public void testFineGrainedMapAfterAtomicMap() throws Exception {
      Cache<String, Object> cache1 = cache(0, "atomic");

      AtomicMap<String, String> map = getAtomicMap(cache1, "testReplicationRemoveCommit");
      FineGrainedAtomicMap<String, String> map2 = getFineGrainedAtomicMap(cache1, "testReplicationRemoveCommit");
   }

   @Override
   protected <CK, K, V> Map<K, V> createAtomicMap(Cache<CK, Object> cache, CK key, boolean createIfAbsent) {
      return getFineGrainedAtomicMap(cache, key, createIfAbsent);
   }
}
