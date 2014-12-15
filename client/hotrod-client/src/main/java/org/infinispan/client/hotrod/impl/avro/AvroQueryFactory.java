package org.infinispan.client.hotrod.impl.avro;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.infinispan.client.hotrod.impl.RemoteCacheImpl;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryBuilder;
import org.infinispan.query.dsl.impl.BaseQueryFactory;

/**
 * @author Pierre Sutra
 * @since 7.0
 */
public class AvroQueryFactory extends BaseQueryFactory<Query> {

   private RemoteCacheImpl cache;
   private Schema schema;

   public AvroQueryFactory(RemoteCacheImpl c){
      this.cache = c;
   }

   @Override
   public QueryBuilder<Query> from(Class entityType) {
      return from(GenericData.Record.class.getName());
   }

   @Override
   public QueryBuilder<Query> from(String entityType) {
      return new AvroQueryBuilder(
            cache,
            this);
   }

}
