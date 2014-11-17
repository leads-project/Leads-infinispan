package org.infinispan.query.impl.externalizers;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.WildcardQuery;
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
public class LuceneWildcardQuery extends AbstractExternalizer<WildcardQuery> {

   @Override
   public Set<Class<? extends WildcardQuery>> getTypeClasses() {
      return Util.<Class<? extends WildcardQuery>>asSet(WildcardQuery.class);
   }

   @Override
   public WildcardQuery readObject(final ObjectInput input) throws IOException, ClassNotFoundException {
      Term term = (Term) input.readObject();
      return new WildcardQuery(term);
   }

   @Override
   public void writeObject(final ObjectOutput output, final WildcardQuery query) throws IOException {
      output.writeObject(query.getTerm());
   }

   @Override
   public Integer getId() {
      return ExternalizerIds.LUCENE_WILDCARD_QUERY;
   }

}
