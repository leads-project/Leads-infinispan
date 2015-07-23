package org.infinispan.client.hotrod.impl.avro;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.impl.operations.RetryOnFailureOperation;
import org.infinispan.client.hotrod.impl.protocol.Codec;
import org.infinispan.client.hotrod.impl.protocol.HeaderParams;
import org.infinispan.client.hotrod.impl.transport.Transport;
import org.infinispan.client.hotrod.impl.transport.TransportFactory;
import org.infinispan.client.hotrod.impl.transport.tcp.TcpTransportFactory;
import org.infinispan.client.hotrod.logging.Log;
import org.infinispan.client.hotrod.logging.LogFactory;
import org.infinispan.query.remote.client.avro.AvroMarshaller;
import org.infinispan.query.remote.client.avro.Request;
import org.infinispan.query.remote.client.avro.Response;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ¨Pierre Sutra
 * @since 4.0
 */
public class AvroQueryOperation extends RetryOnFailureOperation<Response> {

   private static final Log log = LogFactory.getLog(AvroQueryOperation.class, Log.class);
   
   private AvroRemoteQuery remoteQuery;
   private AvroMarshaller<Request> requestAvroMarshaller;
   private AvroMarshaller<Response> responseAvroMarshaller;

   public AvroQueryOperation(Codec codec, TransportFactory transportFactory, byte[] cacheName,
         AtomicInteger topologyId, Flag[] flags, AvroRemoteQuery query) {
      super(codec, transportFactory, cacheName, topologyId, flags);
      this.remoteQuery = query;
      this.requestAvroMarshaller = new AvroMarshaller<>(Request.class);
      this.responseAvroMarshaller = new AvroMarshaller<>(Response.class);
   }

   @Override
   protected Transport getTransport(int retryCount, Set<SocketAddress> failedServers) {

      if (remoteQuery.getLocation()==null)
         return transportFactory.getTransport(failedServers, this.cacheName);

      if (!(transportFactory instanceof  TcpTransportFactory)) {
         log.warn("Unable to satisfy destination=" + remoteQuery.getLocation()+"; not a TCPTransportFactory");
         return transportFactory.getTransport(failedServers, this.cacheName);
      }
      
      Collection<SocketAddress> servvers = ((TcpTransportFactory)transportFactory).getServers();
      
      for (SocketAddress addr : servvers) {
         InetSocketAddress address = (InetSocketAddress) addr;
         if (address.getHostName().equals(remoteQuery.getLocation().getHostName())
               && address.getPort() == remoteQuery.getLocation().getPort() ) {
            if (failedServers != null && failedServers.contains(address)) {
               log.warn("Unable to satisfy destination=" + remoteQuery.getLocation()+"; server failed");
            }
            return transportFactory.getAddressTransport(address);
         }
      }

      log.warn("Unable to satisfy destination=" + remoteQuery.getLocation()+"; server not found");
      return transportFactory.getTransport(failedServers, this.cacheName);
   }

   @Override
   protected Response executeOperation(Transport transport) {
      HeaderParams params = writeHeader(transport, QUERY_REQUEST);
      Request queryRequest = new Request();
      queryRequest.setJpqlString(remoteQuery.getJpqlString());
      queryRequest.setSchemaName(remoteQuery.schemaName.getFullName());
      queryRequest.setStartOffset(remoteQuery.getStartOffset());
      queryRequest.setMaxResult(remoteQuery.getMaxResults());
      queryRequest.setLocal(remoteQuery.getLocation()!=null);

      try {

         transport.writeArray(requestAvroMarshaller.objectToBuffer(queryRequest).getBuf());
         transport.flush();
         readHeaderAndValidate(transport, params);
         byte[] responseBytes = transport.readArray();

         return (Response) responseAvroMarshaller.objectFromByteBuffer(responseBytes);

      } catch (IOException | InterruptedException | ClassNotFoundException e) {
         e.printStackTrace();
      }

      return null;

   }

}
