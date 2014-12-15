package org.infinispan.query.remote.avro;

import org.apache.avro.Schema;
import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.marshall.core.JBossMarshaller;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Pierre Sutra
 * @since 7.0
 */
public class SchemaExternalizer extends AbstractExternalizer<Schema> {

   private JBossMarshaller marshaller;

   public SchemaExternalizer(){
      marshaller = new JBossMarshaller();
   }

   @Override public Set<Class<? extends Schema>> getTypeClasses() {
      Set<Class<? extends Schema>> set = new HashSet<>();
      set.add(Schema.class);
      return set;
   }

   @Override public void writeObject(ObjectOutput output, Schema object) throws IOException {
      // TODO: Customise this generated block
   }

   @Override public Schema readObject(ObjectInput input) throws IOException, ClassNotFoundException {
      return null;  // TODO: Customise this generated block
   }
}
