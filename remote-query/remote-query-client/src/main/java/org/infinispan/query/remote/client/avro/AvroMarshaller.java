package org.infinispan.query.remote.client.avro;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.*;
import org.apache.avro.specific.SpecificDatumReader;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.commons.marshall.AbstractMarshaller;
import org.infinispan.commons.marshall.jboss.GenericJBossMarshaller;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * @author Pierre Sutra
 */
public class AvroMarshaller<T> extends AbstractMarshaller{

   private Class<T> clazz;
   private Schema schema;
   private GenericJBossMarshaller marshaller;
   private SpecificDatumReader<T> reader;

   public AvroMarshaller(Class<T> c) {
      clazz = c;
      marshaller = new GenericJBossMarshaller();
      try {
         schema = getSchema(clazz.newInstance());
      } catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
         e.printStackTrace();
      }
      reader = new SpecificDatumReader<>(clazz);
   }

   @Override
   protected ByteBuffer objectToBuffer(Object o, int estimatedSize) throws IOException, InterruptedException {
      if (!isMarshallable(o)) {
         return marshaller.objectToBuffer(o);
      } else if (o instanceof Schema) {
         return marshaller.objectToBuffer(o.toString());
      } else {
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         ObjectOutputStream oos = new ObjectOutputStream(baos);
         oos.writeObject((schema.getFullName()));
         BinaryEncoder encoder = EncoderFactory.get().directBinaryEncoder(baos, null);
         DatumWriter datumWriter = new GenericDatumWriter(schema);
         datumWriter.write(o, encoder);
         encoder.flush();
         return new ByteBufferImpl(baos.toByteArray(), 0, baos.size());
      }
   }

   @Override
   public Object objectFromByteBuffer(byte[] buf, int offset, int length) throws IOException, ClassNotFoundException {
      try{
         ByteArrayInputStream bais = new ByteArrayInputStream(buf);
         ObjectInputStream ois = new ObjectInputStream(bais);
         Object o =  ois.readObject(); // we skip the schema string
         Decoder decoder = DecoderFactory.get().binaryDecoder( bais, null );
         return reader.read( null, decoder );
      } catch (IOException e) {
         return Schema.parse((String) marshaller.objectFromByteBuffer(buf, offset, length));
      }
   }

   //
   // HELPERS
   //

   private Schema getSchema(Object o) throws InvocationTargetException, IllegalAccessException {
      for(Method method : o.getClass().getMethods()){
         if (method.getName().equals("getSchema"))
            return (Schema) method.invoke(o);
      }
      throw new IllegalAccessException("no such method");
   }

   @Override
   public boolean isMarshallable(Object o) {
      if (o==null)
         return false;
      try {
         if (clazz.isAssignableFrom(o.getClass()) || Schema.class.isAssignableFrom(o.getClass()))
            return true;
      } catch (Exception e) {
         // ignore this
      }
      return false;
   }

}
