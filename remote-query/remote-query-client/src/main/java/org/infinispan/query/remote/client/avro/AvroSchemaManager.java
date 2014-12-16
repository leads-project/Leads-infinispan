package org.infinispan.query.remote.client.avro;

import org.apache.avro.Schema;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.marshall.jboss.GenericJBossMarshaller;

import java.io.IOException;
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

   private BasicCache cache;
   private ConcurrentMap<String, Schema> knownSchemas;
   private GenericJBossMarshaller marshaller;

   public AvroSchemaManager(BasicCache cache){
      this.cache = cache;
      this.knownSchemas = new ConcurrentHashMap<>();
      this.marshaller = new GenericJBossMarshaller();
      knownSchemas.put(Request.getClassSchema().getFullName(),Request.getClassSchema());
      knownSchemas.put(Response.getClassSchema().getFullName(),Response.getClassSchema());
   }

   public Schema retrieveSchema(String name) {
      if (!knownSchemas.containsKey(name)) {
         try {
            ByteBuffer b = marshaller.objectToBuffer(name);
            byte[] bytes = new byte[b.getLength()];
            System.arraycopy(b.getBuf(), b.getOffset(), bytes, 0, b.getLength());
            Schema schema = (Schema) cache.get(bytes);
            knownSchemas.put(name,schema);
         } catch (IOException | InterruptedException e) {
            e.printStackTrace();
         }
      }
      return knownSchemas.get(name);
   }

   public void storeSchema(Schema schema) {
      cache.put(schema.getFullName(), schema);
   }

}
