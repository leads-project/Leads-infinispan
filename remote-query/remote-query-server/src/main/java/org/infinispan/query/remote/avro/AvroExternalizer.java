package org.infinispan.query.remote.avro;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.DatumReader;
import org.infinispan.query.remote.ExternalizerIds;
import org.infinispan.query.remote.client.avro.AvroAbstractMarshaller;

import java.io.IOException;
import java.io.Serializable;

/**
 * @author Pierre Sutra
  */
public class AvroExternalizer extends AvroAbstractMarshaller{

   public AvroExternalizer(){}

   // Others

   protected DatumReader reader(String schemaName)
         throws InterruptedException, IOException, ClassNotFoundException {
      Schema schema = AvroMetadataManager.getInstance().retrieveSchema(schemaName);
      return new GenericDatumReader<>(schema);
   }

   @Override
   public boolean isMarshallable(Object o) throws Exception {
      return (o instanceof GenericData.Record) || (o instanceof Schema) || (o instanceof Serializable);
   }

   @Override
   public Integer getId() {
      return ExternalizerIds.AVRO_VALUE_WRAPPER;
   }

}
