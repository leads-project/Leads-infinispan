package org.infinispan.query.clustered;

import org.apache.lucene.search.TopDocs;

import java.util.Map;

/**
 * ISPNEagerTopDocs.
 * 
 * A TopDocs with an array with keys of each result.
 * 
 * @author Israel Lacerra <israeldl@gmail.com>
 * @since 5.1
 */
public class ISPNEagerTopDocs extends TopDocs {

   private static final long serialVersionUID = 3236786895259278399L;

   public Map<Integer, Object> keys;

   public ISPNEagerTopDocs(TopDocs topDocs, Map<Integer, Object> keys) {
      super(topDocs.totalHits, topDocs.scoreDocs, topDocs.getMaxScore());
      this.keys = keys;
   }

}
