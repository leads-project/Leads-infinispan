package org.infinispan.client.hotrod.impl.avro;

import org.apache.avro.generic.GenericData;
import org.infinispan.client.hotrod.impl.RemoteCacheImpl;
import org.infinispan.client.hotrod.impl.transport.tcp.TcpTransportFactory;
import org.infinispan.client.hotrod.logging.Log;
import org.infinispan.client.hotrod.logging.LogFactory;
import org.infinispan.commons.CacheException;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.dsl.impl.BaseQueryBuilder;
import org.infinispan.query.dsl.impl.JPAQueryGenerator;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author Pierre Sutra
 * @since 7.0
 */
public class AvroQueryBuilder extends BaseQueryBuilder<Query> {

   private static final Log log = LogFactory.getLog(AvroQueryBuilder.class);

   private RemoteCacheImpl cache;

   public AvroQueryBuilder(RemoteCacheImpl c, QueryFactory qf) {
      super(qf,GenericData.Record.class.getName());
      cache = c;
   }

   @Override
   public Query build() {
      String jpqlString = accept(new JPAQueryGenerator());
      log.debug("JPQL string : "+jpqlString);
      return new AvroRemoteQuery(cache, jpqlString, startOffset, maxResults);
   }

   public Collection<AvroRemoteQuery> split(Query query) {
      if (!(query instanceof AvroRemoteQuery))
         throw new CacheException("need an AvroRemoteQuery");
      if (!(cache.getTransportFactory() instanceof  TcpTransportFactory))
         throw new CacheException("need a TcpTransportFactory");
      Collection<SocketAddress> servers = ((TcpTransportFactory)cache.getTransportFactory()).getServers();
      Collection<AvroRemoteQuery> results = new ArrayList<>();
      for (SocketAddress addr: servers) {
         InetSocketAddress address = (InetSocketAddress) addr;
         try {
            AvroRemoteQuery q = (AvroRemoteQuery) ((AvroRemoteQuery) query).clone();
            q.setLocation(address);
            results.add(q);
         } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            // not reachable.
         }
      }
      return results;
   }

}
