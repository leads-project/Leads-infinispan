package org.infinispan.query.remote.avro;

import org.apache.avro.Schema;
import org.infinispan.Cache;
import org.infinispan.commons.equivalence.ByteArrayEquivalence;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.query.remote.client.avro.AvroSupport;
import org.infinispan.query.remote.client.avro.Request;
import org.infinispan.query.remote.client.avro.Response;
import org.infinispan.transaction.LockingMode;
import org.infinispan.util.concurrent.IsolationLevel;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * @author Pierre Sutra
 * @since 7.0
 */
public class AvroMetadataManager {

   private static AvroMetadataManager instance;
   public static AvroMetadataManager getInstance(){
      return instance;
   }
   public static void setInstance(AvroMetadataManager instance){
      AvroMetadataManager.instance = instance;
   }

   private Cache<byte[], byte[]> cache;
   private ConcurrentMap<String, Schema> knownSchemas;
   private Marshaller marshaller;

   public AvroMetadataManager(DefaultCacheManager cacheManager){
      Configuration cacheConfiguration = cacheManager.getCacheConfiguration(AvroSupport.AVRO_METADATA_CACHE_NAME);
      if (cacheConfiguration == null) {
         ConfigurationBuilder cfg = new ConfigurationBuilder();
         CacheMode cacheMode = CacheMode.REPL_SYNC;  // FIXME
         cfg.transaction().lockingMode(LockingMode.PESSIMISTIC).syncCommitPhase(true).syncRollbackPhase(true)
               .locking().isolationLevel(IsolationLevel.READ_COMMITTED).useLockStriping(false)
               .clustering().cacheMode(cacheMode)
               .stateTransfer().fetchInMemoryState(true).awaitInitialTransfer(false)
               .dataContainer().keyEquivalence(new ByteArrayEquivalence()); // for HotRod compatibility
         cacheManager.defineConfiguration(AvroSupport.AVRO_METADATA_CACHE_NAME, cfg.build());
      }
      this.cache = cacheManager.getCache(AvroSupport.AVRO_METADATA_CACHE_NAME);
      this.cache.start();

      this.knownSchemas = new ConcurrentHashMap<>();
      knownSchemas.put(Request.getClassSchema().getFullName(),Request.getClassSchema());
      knownSchemas.put(Response.getClassSchema().getFullName(),Response.getClassSchema());

      this.marshaller= new AvroExternalizer();
   }

   public Schema retrieveSchema(String name) throws IOException, InterruptedException, ClassNotFoundException {
      if (!knownSchemas.containsKey(name)) {
         byte[] key = marshaller.objectToByteBuffer(name);
         byte[] value = cache.get(key);
         assert value!=null : name;
         Schema schema = (Schema) marshaller.objectFromByteBuffer(value);
         knownSchemas.put(name,schema);
      }
      return knownSchemas.get(name);
   }

}
