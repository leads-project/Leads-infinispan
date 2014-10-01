package org.infinispan.client.hotrod.impl.avro;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.impl.operations.RetryOnFailureOperation;
import org.infinispan.client.hotrod.impl.protocol.Codec;
import org.infinispan.client.hotrod.impl.protocol.HeaderParams;
import org.infinispan.client.hotrod.impl.transport.Transport;
import org.infinispan.client.hotrod.impl.transport.TransportFactory;
import org.infinispan.query.remote.client.avro.AvroMarshaller;
import org.infinispan.query.remote.client.avro.Request;
import org.infinispan.query.remote.client.avro.Response;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * // TODO: Document this
 *
 * @author otrack
 * @since 4.0
 */
public class AvroQueryOperation extends RetryOnFailureOperation<Response> {

    private AvroRemoteQuery remoteQuery;

    public AvroQueryOperation(Codec codec, TransportFactory transportFactory, byte[] cacheName,
                              AtomicInteger topologyId, Flag[] flags, AvroRemoteQuery query) {
        super(codec, transportFactory, cacheName, topologyId, flags);
        this.remoteQuery = query;
    }

    @Override
    protected Transport getTransport(int retryCount, Set<SocketAddress> failedServers) {
        return transportFactory.getTransport(failedServers);
    }

    @Override
    protected Response executeOperation(Transport transport) {
        HeaderParams params = writeHeader(transport, QUERY_REQUEST);
        Request queryRequest = new Request();
        queryRequest.setJpqlString(remoteQuery.getJpqlString());
        queryRequest.setStartOffset(remoteQuery.getStartOffset());
        queryRequest.setMaxResult(remoteQuery.getMaxResults());

        try {

            AvroMarshaller<Request> requestAvroMarshaller = new AvroMarshaller<>(Request.class);
            transport.writeArray(requestAvroMarshaller.objectToBuffer(queryRequest).getBuf());
            transport.flush();
            readHeaderAndValidate(transport, params);
            byte[] responseBytes = transport.readArray();
            AvroMarshaller<Response> responseAvroMarshaller = new AvroMarshaller<>(Response.class);
            return (Response) responseAvroMarshaller.objectFromByteBuffer(responseBytes);

        } catch (IOException | InterruptedException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;

    }

}
