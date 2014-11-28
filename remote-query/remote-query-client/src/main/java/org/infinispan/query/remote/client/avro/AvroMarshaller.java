package org.infinispan.query.remote.client.avro;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.file.SeekableByteArrayInput;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.util.Utf8;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.commons.marshall.AbstractMarshaller;
import org.infinispan.commons.marshall.jboss.GenericJBossMarshaller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 *
 * @author otrack
 * @since 4.0
 */
public class AvroMarshaller<T> extends AbstractMarshaller{

   private Class<T> clazz;
   private Schema schema;
   private GenericJBossMarshaller marshaller = new GenericJBossMarshaller();

   public AvroMarshaller(Class<T> c) {
      clazz = c;
      try {
         schema = getSchema(clazz.newInstance());
      } catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
         e.printStackTrace();
      }
   }

   @Override
   protected ByteBuffer objectToBuffer(Object o, int estimatedSize) throws IOException, InterruptedException {
      if (!isMarshallable(o)){
         return marshaller.objectToBuffer(o);
      }else if (o instanceof Utf8){
         return marshaller.objectToBuffer(o.toString());
      }else{
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         GenericDatumWriter<T> writer = new GenericDatumWriter<>(schema);
         DataFileWriter<T> dataFileWriter = new DataFileWriter<>(writer);
         dataFileWriter.create(schema,baos);
         dataFileWriter.append((T) o);
         dataFileWriter.close();
         return new ByteBufferImpl(baos.toByteArray(),0,baos.size());
      }
   }

   @Override
   public Object objectFromByteBuffer(byte[] buf, int offset, int length) throws IOException, ClassNotFoundException {
      try{
         Object ret = null;
         SpecificDatumReader<T> reader = new SpecificDatumReader<>(clazz);
         DataFileReader<T> dataFileReader = new DataFileReader<>(new SeekableByteArrayInput(buf),reader);
         if(dataFileReader.hasNext())
            ret = dataFileReader.next();
         dataFileReader.close();
         return ret;
      } catch (IOException e) {
         return marshaller.objectFromByteBuffer(buf, offset, length);
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
         if (clazz.isAssignableFrom(o.getClass()) || Utf8.class.isAssignableFrom(o.getClass()))
            return true;
      } catch (Exception e) {
         // ignore this
      }
      return false;
   }

}
