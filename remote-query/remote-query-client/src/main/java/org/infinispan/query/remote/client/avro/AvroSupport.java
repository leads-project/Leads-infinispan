package org.infinispan.query.remote.client.avro;

import org.apache.avro.Schema;
import org.infinispan.commons.api.BasicCache;

/**
 * @author Pierre Sutra
 */
public class AvroSupport {

   public static void registerSchema(BasicCache cache, Schema schema) {
      cache.put(schema.getFullName(), schema);
   }

}
