package org.infinispan.query.remote.avro;

import org.apache.avro.Schema;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.marshall.jboss.GenericJBossMarshaller;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.TransactionMode;
import org.infinispan.interceptors.locking.PessimisticLockingInterceptor;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.query.remote.CompatibilityProtoStreamMarshaller;
import org.infinispan.query.remote.ProtobufMetadataManagerInterceptor;
import org.infinispan.query.remote.client.avro.AvroMarshaller;
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
public class AvroSchemaManager {

   public static final String AVRO_METADATA_CACHE_NAME = "__avro_metadata";
   private static AvroSchemaManager instance;
   public static AvroSchemaManager getInstance(){
      return instance;
   }
   public static void setInstance(AvroSchemaManager instance){
      AvroSchemaManager.instance = instance;
   }

   private BasicCache cache;
   private ConcurrentMap<String, Schema> knownSchemas;
   private GenericJBossMarshaller marshaller;

   public AvroSchemaManager(DefaultCacheManager cacheManager){
      Configuration cacheConfiguration = cacheManager.getCacheConfiguration(AVRO_METADATA_CACHE_NAME);
      if (cacheConfiguration == null) {
         ConfigurationBuilder cfg = new ConfigurationBuilder();
         cfg.transaction()
               .transaction().lockingMode(LockingMode.PESSIMISTIC).syncCommitPhase(true).syncRollbackPhase(true)
               .locking().isolationLevel(IsolationLevel.READ_COMMITTED).useLockStriping(false)
               .clustering().sync()
               .stateTransfer().fetchInMemoryState(true).awaitInitialTransfer(false)
               .compatibility().enable().marshaller(new GenericRecordExternalizer());
         cacheManager.defineConfiguration(AVRO_METADATA_CACHE_NAME, cfg.build());
      }
      this.cache = cacheManager.getCache(AVRO_METADATA_CACHE_NAME);
      this.knownSchemas = new ConcurrentHashMap<>();
      this.marshaller = new GenericJBossMarshaller();
      knownSchemas.put(Request.getClassSchema().getFullName(),Request.getClassSchema());
      knownSchemas.put(Response.getClassSchema().getFullName(),Response.getClassSchema());
   }

   public Schema retrieveSchema(String name) {
      if (!knownSchemas.containsKey(name)) {
         Schema schema = (Schema) cache.get(name);
         assert schema!=null;
         knownSchemas.put(name,schema);
      }
      return knownSchemas.get(name);
   }

}
