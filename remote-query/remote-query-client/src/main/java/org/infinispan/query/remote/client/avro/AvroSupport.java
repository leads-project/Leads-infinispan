package org.infinispan.query.remote.client.avro;

import org.apache.avro.Schema;
import org.infinispan.commons.api.BasicCacheContainer;

/**
 * @author Pierre Sutra
 */
public class AvroSupport {

   public static final String AVRO_METADATA_CACHE_NAME = "__avro_metadata";

   public static void registerSchema(BasicCacheContainer container, Schema schema) {
      container.getCache(AVRO_METADATA_CACHE_NAME).put(schema.getFullName(), schema);
   }

}
