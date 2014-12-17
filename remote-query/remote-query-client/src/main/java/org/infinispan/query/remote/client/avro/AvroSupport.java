package org.infinispan.query.remote.client.avro;

import org.apache.avro.Schema;
import org.infinispan.commons.api.BasicCacheContainer;

/**
 * @author Pierre Sutra
 */
public class AvroSupport {

   public static void registerSchema(BasicCacheContainer container, Schema schema) {
      container.getCache("__avro_metadata").put(schema.getFullName(), schema);
   }

}
