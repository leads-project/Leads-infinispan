package org.infinispan.client.hotrod.impl.avro;

import org.apache.avro.generic.GenericData;
import org.infinispan.client.hotrod.impl.RemoteCacheImpl;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryBuilder;
import org.infinispan.query.dsl.impl.BaseQueryFactory;

/**
 * // TODO: Document this
 *
 * @author otrack
 * @since 4.0
 */
public class AvroQueryFactory extends BaseQueryFactory<Query> {

    private RemoteCacheImpl cache;

    public AvroQueryFactory(RemoteCacheImpl c){
        cache = c;
    }

    @Override
    public QueryBuilder<Query> from(Class entityType) {
        return from(GenericData.Record.class.getName());
    }

    @Override
    public QueryBuilder<Query> from(String entityType) {
        return new AvroQueryBuilder(cache, this, GenericData.Record.class.getName());
    }

}
