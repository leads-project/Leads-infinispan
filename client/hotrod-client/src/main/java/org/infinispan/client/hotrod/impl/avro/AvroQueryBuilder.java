package org.infinispan.client.hotrod.impl.avro;

import org.infinispan.client.hotrod.impl.RemoteCacheImpl;
import org.infinispan.client.hotrod.logging.Log;
import org.infinispan.client.hotrod.logging.LogFactory;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.impl.BaseQueryBuilder;
import org.infinispan.query.dsl.impl.JPAQueryGenerator;
/**
 * // TODO: Document this
 *
 * @author otrack
 * @since 4.0
 */
public class AvroQueryBuilder extends BaseQueryBuilder<Query> {

    private static final Log log = LogFactory.getLog(AvroQueryBuilder.class);

    private RemoteCacheImpl cache;

    public AvroQueryBuilder(RemoteCacheImpl c, String entityType) {
        super(entityType);
        cache = c;
    }

    @Override
    public Query build() {
        String jpqlString = accept(new JPAQueryGenerator());
        log.tracef("JPQL string : %s", jpqlString);
        return new AvroRemoteQuery(cache, jpqlString, startOffset, maxResults);
    }

}
