package org.infinispan.query.remote.client.avro;

import org.apache.avro.generic.GenericContainer;

/**
 * @author Pierre Sutra
 */
public class AvroMarshaller<T extends GenericContainer> extends AvroSpecificMarshaller{

   public AvroMarshaller(Class c) {
      super(c);
   }

}
