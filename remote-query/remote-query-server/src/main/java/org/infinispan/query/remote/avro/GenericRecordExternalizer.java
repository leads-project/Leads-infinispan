package org.infinispan.query.remote.avro;

import org.apache.avro.file.DataFileStream;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.infinispan.commons.io.UnsignedNumeric;
import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.marshall.core.JBossMarshaller;
import org.infinispan.query.remote.ExternalizerIds;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Pierre Sutra
  */
public class GenericRecordExternalizer extends AbstractExternalizer<GenericData.Record> {

   private JBossMarshaller marshaller = new JBossMarshaller();

   public GenericRecordExternalizer(){
   }

   @Override
   public Set<Class<? extends GenericData.Record>> getTypeClasses() {
      Set<Class<? extends GenericData.Record>> set = new HashSet<>();
      set.add(GenericData.Record.class);
      return set;
   }

   @Override
   public void writeObject(ObjectOutput output, GenericData.Record record) throws IOException {
      byte[] out = objectToByteBuffer(record);
      UnsignedNumeric.writeUnsignedInt(output, out.length);
      output.write(out);
   }

   @Override
   public GenericData.Record readObject(ObjectInput input) throws IOException, ClassNotFoundException {
      int len = UnsignedNumeric.readUnsignedInt(input);
      byte[] in = new byte[len];
      input.readFully(in);
      return (GenericData.Record) objectFromByteBuffer(in);
   }

   public byte[] objectToByteBuffer(Object o) throws IOException{
      if (o instanceof GenericData.Record){
         GenericData.Record record = (GenericData.Record) o;
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         DatumWriter<GenericData.Record> datumWriter = new GenericDatumWriter<>(record.getSchema());
         DataFileWriter<GenericData.Record> writer = new DataFileWriter<>(datumWriter);
         writer.create(record.getSchema(),baos);
         writer.append(record);
         writer.close();
         return baos.toByteArray();
      }
      try {
         return marshaller.objectToByteBuffer(o);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
      return null;
   }

   public Object objectFromByteBuffer(byte[] buf) throws IOException, ClassNotFoundException {
      try {
         ByteArrayInputStream bais = new ByteArrayInputStream(buf);
         DatumReader<GenericData.Record> reader = new GenericDatumReader<>();
         DataFileStream<GenericData.Record> stream = null;
         stream = new DataFileStream<>(bais,reader);
         if(!stream.hasNext())
            throw new IOException("Stream is empty.");
         Object ret = stream.next();
         stream.close();
         return ret;
      } catch (IOException e) {
         e.printStackTrace();
      }
      return marshaller.objectFromByteBuffer(buf);

   }

   @Override
   public Integer getId() {
      return ExternalizerIds.AVRO_VALUE_WRAPPER;
   }

}
