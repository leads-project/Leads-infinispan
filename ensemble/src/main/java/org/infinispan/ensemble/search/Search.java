package org.infinispan.ensemble.search;

import org.infinispan.client.hotrod.impl.avro.AvroQueryFactory;
import org.infinispan.commons.CacheException;
import org.infinispan.ensemble.cache.EnsembleCache;
import org.infinispan.ensemble.cache.SiteEnsembleCache;
import org.infinispan.ensemble.cache.distributed.DistributedEnsembleCache;
import org.infinispan.ensemble.cache.replicated.ReplicatedEnsembleCache;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;

import java.util.List;


/**
  * @author Pierre Sutra
 */
public class Search {

    private Search() {
    }

    public static QueryFactory<Query> getQueryFactory(EnsembleCache cache) {

        if ( cache instanceof DistributedEnsembleCache){
            DistributedEnsembleCache distributedEnsembleCache =  ((DistributedEnsembleCache)cache);
            if (distributedEnsembleCache.isFrontierMode())
                return Search.getQueryFactory(((DistributedEnsembleCache) cache).getFrontierCache());
        } else if (cache instanceof ReplicatedEnsembleCache) {
            List<SiteEnsembleCache> list = cache.getCaches();
            for (SiteEnsembleCache c : list) {
                if (c.isLocal())
                    return new AvroQueryFactory(c.getDelegeate());
            }
        } else if (cache instanceof SiteEnsembleCache){
            return new AvroQueryFactory(((SiteEnsembleCache) cache).getDelegeate());
        }

        throw new CacheException("Unsupported Ensemble cache type and parameters.");

    }

}
