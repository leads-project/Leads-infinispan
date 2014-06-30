package org.infinispan.query.remote.avro;

import org.apache.avro.generic.GenericData;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Norms;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.cfg.SearchMapping;
import org.infinispan.Cache;
import org.infinispan.query.remote.indexing.AvroValueWrapperFieldBridge;
import org.infinispan.query.spi.ProgrammaticSearchMappingProvider;

/**
 * // TODO: Document this
 *
 * @author otrack
 * @since 4.0
 */
public class SearchMappingProviderImpl implements ProgrammaticSearchMappingProvider {
    @Override
    public void defineMappings(Cache cache, SearchMapping searchMapping) {
        searchMapping.entity(GenericData.Record.class)
                .indexed()
                .classBridgeInstance(new AvroValueWrapperFieldBridge())
                .norms(Norms.NO)
                .analyze(Analyze.YES)
                .store(Store.YES);
    }
}
