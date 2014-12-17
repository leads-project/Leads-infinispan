package org.infinispan.query.remote.avro;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.*;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.commons.io.UnsignedNumeric;
import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.marshall.BufferSizePredictor;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.marshall.jboss.GenericJBossMarshaller;
import org.infinispan.query.remote.ExternalizerIds;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Pierre Sutra
  */
public class GenericRecordExternalizer extends AbstractExternalizer<GenericData.Record> implements Marshaller{

   private GenericJBossMarshaller marshaller;

   public GenericRecordExternalizer(){
      marshaller = new GenericJBossMarshaller();
   }

   @Override
   public Set<Class<? extends GenericData.Record>> getTypeClasses() {
      Set<Class<? extends GenericData.Record>> set = new HashSet<>();
      set.add(GenericData.Record.class);
      return set;
   }

   @Override
   public void writeObject(ObjectOutput output, GenericData.Record record) throws IOException {
      output.writeObject(record.getSchema().getFullName());
      byte[] out = objectToByteBuffer(record);
      UnsignedNumeric.writeUnsignedInt(output, out.length);
      output.write(out);
   }

   @Override
   public GenericData.Record readObject(ObjectInput input) throws IOException, ClassNotFoundException {
      Schema schema = AvroSchemaManager.getInstance()
            .retrieveSchema((String) input.readObject());
      int len = UnsignedNumeric.readUnsignedInt(input);
      byte[] in = new byte[len];
      input.readFully(in);
      return (GenericData.Record) objectFromByteBuffer(in,schema);
   }

   @Override public byte[] objectToByteBuffer(Object obj, int estimatedSize) throws IOException, InterruptedException {
      return objectToByteBuffer(obj);
   }

   public byte[] objectToByteBuffer(Object o) throws IOException{
      if (o instanceof GenericData.Record) {
         GenericData.Record record = (GenericData.Record) o;
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         ObjectOutputStream oos = new ObjectOutputStream(baos);
         oos.writeObject((record.getSchema().getFullName()));
         DatumWriter<GenericData.Record> datumWriter = new GenericDatumWriter<>(record.getSchema());
         BinaryEncoder encoder = EncoderFactory.get().directBinaryEncoder( baos, null );
         datumWriter.write(record,encoder);
         encoder.flush();
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
      try{
         ByteArrayInputStream bais = new ByteArrayInputStream(buf);
         ObjectInputStream ois = new ObjectInputStream(bais);
         Schema schema = AvroSchemaManager.getInstance().retrieveSchema((String) ois.readObject());
         Decoder decoder = DecoderFactory.get().binaryDecoder( bais, null );
         GenericDatumReader<GenericData.Record> reader = new GenericDatumReader<>(schema);
         return reader.read( null, decoder );
      } catch (Exception e) {
         Object o = marshaller.objectFromByteBuffer(buf);
         try {
            return Schema.parse((String)o);
         } catch (Exception e1) {
            return o;
         }
      }
   }

   @Override public Object objectFromByteBuffer(byte[] buf, int offset, int length)
         throws IOException, ClassNotFoundException {
      return objectFromByteBuffer(buf);
   }

   @Override public ByteBuffer objectToBuffer(Object o) throws IOException, InterruptedException {
      byte[] buf = objectToByteBuffer(o);
      return new ByteBufferImpl(buf, 0, buf.length);
   }

   @Override public boolean isMarshallable(Object o) throws Exception {
      return true;
   }

   @Override public BufferSizePredictor getBufferSizePredictor(Object o) {
      return null;  // TODO: Customise this generated block
   }

   public Object objectFromByteBuffer(byte[] buf, Schema schema) throws IOException, ClassNotFoundException {
      ByteArrayInputStream bais = new ByteArrayInputStream(buf);
      Decoder decoder = DecoderFactory.get().binaryDecoder( bais, null );
      GenericDatumReader<GenericData.Record> reader = new GenericDatumReader<>(schema);
      return reader.read( null, decoder );
   }

   @Override
   public Integer getId() {
      return ExternalizerIds.AVRO_VALUE_WRAPPER;
   }

}
