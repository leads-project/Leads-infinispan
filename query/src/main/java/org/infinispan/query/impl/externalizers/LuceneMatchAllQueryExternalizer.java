package org.infinispan.query.impl.externalizers;


import org.apache.lucene.search.MatchAllDocsQuery;
import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.util.Util;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

/**
 * @author Pierre Sutra
 * @since 7.0
 */
public class LuceneMatchAllQueryExternalizer extends AbstractExternalizer<MatchAllDocsQuery> {

   @Override
   public Set<Class<? extends MatchAllDocsQuery>> getTypeClasses() {
      return Util.<Class<? extends MatchAllDocsQuery>>asSet(MatchAllDocsQuery.class);
   }

   @Override
   public MatchAllDocsQuery readObject(final ObjectInput input) throws IOException, ClassNotFoundException {
      final float boost = input.readFloat();
      MatchAllDocsQuery q = new MatchAllDocsQuery();
      q.setBoost(boost);
      return q;
   }

   @Override
   public void writeObject(final ObjectOutput output, final MatchAllDocsQuery query) throws IOException {
      output.writeFloat(query.getBoost());
   }

   @Override
   public Integer getId() {
      return ExternalizerIds.LUCENE_QUERY_MATCH_ALL;
   }

}
