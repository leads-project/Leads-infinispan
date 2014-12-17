package org.infinispan.query.remote.client.avro;

import org.apache.avro.generic.GenericContainer;
import org.apache.avro.io.DatumReader;
import org.apache.avro.specific.SpecificDatumReader;

import java.io.IOException;

/**
 * @author Pierre Sutra
 */
public class AvroSpecificMarshaller<T extends GenericContainer> extends AvroAbstractMarshaller {

   private SpecificDatumReader<T> reader;

   public AvroSpecificMarshaller(Class<T> c) {
      reader = new SpecificDatumReader<>(c);
   }

   @Override
   protected DatumReader reader(String schemaName)
         throws InterruptedException, IOException, ClassNotFoundException {
      return reader;
   }

}
