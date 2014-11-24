package org.infinispan.query.remote.avro;

import org.infinispan.commons.io.UnsignedNumeric;
import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.query.remote.ExternalizerIds;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;


/**
 * This is used to wrap binary values encoded with Avro .
 * AvroValueWrapperFieldBridge is used as a class bridge to allow indexing of the binary payload.
 *
 * @author Pierre Sutra
 * @since 6.0
 */

public final class AvroValueWrapper {

   private final byte[] binary;
   private int hashCode = 0;

   public AvroValueWrapper(byte[] binary) {
      this.binary = binary;
   }

   public byte[] getBinary() {
      return binary;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      AvroValueWrapper that = (AvroValueWrapper) o;
      return Arrays.equals(binary, that.binary);
   }

   @Override
   public int hashCode() {
      if (hashCode == 0) {
         hashCode = Arrays.hashCode(binary);
      }
      return hashCode;
   }

   @Override
   public String toString() {
      return "AvroValueWrapper(" + Arrays.toString(binary) + ')';
   }

   public static final class Externalizer extends AbstractExternalizer<AvroValueWrapper> {

      @Override
      public void writeObject(ObjectOutput output, AvroValueWrapper avroValueWrapper) throws IOException {
         UnsignedNumeric.writeUnsignedInt(output, avroValueWrapper.getBinary().length);
         output.write(avroValueWrapper.getBinary());
      }

      @Override
      public AvroValueWrapper readObject(ObjectInput input) throws IOException {
         int length = UnsignedNumeric.readUnsignedInt(input);
         byte[] binary = new byte[length];
         input.readFully(binary);
         return new AvroValueWrapper(binary);
      }

      @Override
      public Integer getId() {
         return ExternalizerIds.AVRO_VALUE_WRAPPER;
      }

      @Override
      public Set<Class<? extends AvroValueWrapper>> getTypeClasses() {
         return Collections.<Class<? extends AvroValueWrapper>>singleton(AvroValueWrapper.class);
      }
   }

}
