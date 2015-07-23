package org.infinispan.client.hotrod.impl.avro;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericContainer;
import org.apache.avro.generic.GenericData;
import org.infinispan.client.hotrod.impl.RemoteCacheImpl;
import org.infinispan.commons.CacheException;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryBuilder;
import org.infinispan.query.dsl.impl.BaseQueryFactory;

/**
 * @author Pierre Sutra
 * @since 7.0
 */
public class AvroQueryFactory extends BaseQueryFactory<Query> {

   private RemoteCacheImpl cache;

   public AvroQueryFactory(RemoteCacheImpl c){
      this.cache = c;
   }

   @Override
   public QueryBuilder<Query> from(Class entityType) {
      if (!GenericContainer.class.isAssignableFrom(entityType))
         throw new CacheException();
      try {
         GenericContainer container = (GenericContainer) entityType.newInstance();
         Schema schema = container.getSchema();
         return new AvroQueryBuilder(cache,this,schema);
      } catch (InstantiationException | IllegalAccessException e) {
         e.printStackTrace();
      }
      throw new CacheException();
   }

   @Override
   public QueryBuilder<Query> from(String entityType) {
      try {
         return from(Class.forName(entityType));
      } catch (ClassNotFoundException e) {
         e.printStackTrace();
      }
      throw new CacheException();
   }

}
