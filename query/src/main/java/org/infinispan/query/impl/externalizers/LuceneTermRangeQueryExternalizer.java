package org.infinispan.query.impl.externalizers;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermRangeQuery;
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
public class LuceneTermRangeQueryExternalizer  extends AbstractExternalizer<TermRangeQuery> {

   @Override
   public Set<Class<? extends TermRangeQuery>> getTypeClasses() {
      return Util.<Class<? extends TermRangeQuery>>asSet(TermRangeQuery.class);
   }

   @Override
   public TermRangeQuery readObject(final ObjectInput input) throws IOException, ClassNotFoundException {

      final String field = (String) input.readObject();

      final boolean includeLower  = input.readBoolean();
      final BytesRef lowerTerm = (includeLower==true) ? (BytesRef) input.readObject() : null;

      final boolean includeUpper  = input.readBoolean();
      final BytesRef upperTerm = (includeUpper==true) ? (BytesRef) input.readObject() : null;

      TermRangeQuery termRangeQuery = new TermRangeQuery(
            field, lowerTerm, upperTerm, includeLower, includeUpper);

      return termRangeQuery;
   }

   @Override
   public void writeObject(final ObjectOutput output, final TermRangeQuery query) throws IOException {

      output.writeObject(query.getField());

      output.writeBoolean(query.includesLower());
      if (query.includesLower())
         output.writeObject(query.getLowerTerm());

      output.writeBoolean(query.includesUpper());
      if (query.includesUpper())
         output.writeObject(query.getUpperTerm());
   }

   @Override
   public Integer getId() {
      return ExternalizerIds.LUCENE_QUERY_RANGE_TERM;
   }

}
