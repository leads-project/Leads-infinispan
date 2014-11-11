package org.infinispan.query.clustered.commandworkers;

import org.apache.lucene.search.TopDocs;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.query.engine.spi.DocumentExtractor;
import org.infinispan.query.backend.KeyTransformationHandler;
import org.infinispan.query.clustered.ISPNEagerTopDocs;
import org.infinispan.query.clustered.QueryResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * CQCreateEagerQuery.
 * 
 * Returns the results of a node to create a eager distributed iterator.
 * 
 * @author Israel Lacerra <israeldl@gmail.com>
 * @since 5.1
 */
public class CQCreateEagerQuery extends ClusteredQueryCommandWorker {

   @Override
   public QueryResponse perform() {
      query.afterDeserialise((SearchFactoryImplementor) getSearchFactory());
      DocumentExtractor extractor = query.queryDocumentExtractor();
      try {
         int resultSize = query.queryResultSize();
         if (resultSize==0)
            return new QueryResponse(null,null,0);
         ISPNEagerTopDocs eagerTopDocs = collectKeys(extractor);
         QueryResponse queryResponse = new QueryResponse(eagerTopDocs, getQueryBox().getMyId(), resultSize);
         queryResponse.setAddress(cache.getAdvancedCache().getRpcManager().getAddress());
         return queryResponse;
      }
      finally {
         extractor.close();
      }
   }

   private ISPNEagerTopDocs collectKeys(DocumentExtractor extractor) {
      TopDocs topDocs = extractor.getTopDocs();

      Map<Integer, Object> keys = new HashMap<>(topDocs.scoreDocs.length);
      KeyTransformationHandler keyTransformationHandler = KeyTransformationHandler
            .getInstance(cache.getAdvancedCache());

      // collecting keys (it's a eager query!)
      for (int i = 0; i < topDocs.scoreDocs.length; i++) {
         Object key = QueryExtractorUtil.extractKey(extractor, cache, keyTransformationHandler, i);
         keys.put(topDocs.scoreDocs[i].doc,key);
      }

      return new ISPNEagerTopDocs(topDocs, keys);
   }

}
