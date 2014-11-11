package org.infinispan.query.impl.externalizers;

import org.apache.lucene.util.BytesRef;
import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.util.Util;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

/**
 *
 * @author Pierre Sutra
 * @since 7.0
 */
public class LuceneBytesRefExternalizer extends AbstractExternalizer<BytesRef> {

   @Override
   public Set<Class<? extends BytesRef>> getTypeClasses() {
      return Util.<Class<? extends BytesRef>>asSet(BytesRef.class);
   }

   @Override
   public BytesRef readObject(final ObjectInput input) throws IOException, ClassNotFoundException {
      byte[] bytes = (byte[]) input.readObject();
      int length = input.readInt();
      int offset = input.readInt();
      return new BytesRef(bytes,offset,length);
   }

   @Override
   public void writeObject(final ObjectOutput output, final BytesRef bytesRef) throws IOException {
      output.writeObject(bytesRef.bytes);
      output.writeInt(bytesRef.length);
      output.writeInt(bytesRef.offset);
   }

   @Override
   public Integer getId() {
      return ExternalizerIds.LUCENE_QUERY_MATCH_ALL;
   }

}
