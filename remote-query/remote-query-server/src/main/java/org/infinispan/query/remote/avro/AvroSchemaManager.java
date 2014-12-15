package org.infinispan.query.remote.avro;

import org.apache.avro.Schema;
import org.infinispan.manager.CacheContainer;
import org.infinispan.query.remote.client.avro.Request;
import org.infinispan.query.remote.client.avro.Response;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Pierre Sutra
 * @since 7.0
 */
public class AvroSchemaManager {

   private static AvroSchemaManager instance;
   public static AvroSchemaManager getInstance(){
      return instance;
   }
   public static void setInstance(AvroSchemaManager instance){
      AvroSchemaManager.instance = instance;
   }

   private CacheContainer container;
   private ConcurrentMap<String, Schema> knownSchemas;

   public AvroSchemaManager(CacheContainer container){
      this.container = container;
      this.knownSchemas = new ConcurrentHashMap<>();
      knownSchemas.put(Request.getClassSchema().getFullName(),Request.getClassSchema());
      knownSchemas.put(Response.getClassSchema().getFullName(),Response.getClassSchema());
   }

   public Schema retrieveSchema(String name) {
      if (!knownSchemas.containsKey(name))
         knownSchemas.putIfAbsent(name,(Schema)container.getCache().get(name));
      return knownSchemas.get(name);
   }

   public void storeSchema(Schema schema) {
      container.getCache().put(schema.getFullName(),schema);
   }

}
