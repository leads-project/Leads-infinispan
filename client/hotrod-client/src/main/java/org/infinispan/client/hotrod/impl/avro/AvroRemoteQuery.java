package org.infinispan.client.hotrod.impl.avro;

import org.infinispan.client.hotrod.impl.RemoteCacheImpl;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.remote.client.avro.Response;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * // TODO: Document this
 *
 * @author otrack
 * @since 4.0
 */
public class AvroRemoteQuery implements Query {

    protected final RemoteCacheImpl cache;
    protected final String jpqlString;
    protected final long startOffset; //todo can this really be long or it has to be int due to limitations in query module?
    protected final int maxResults;

    protected List results = null;
    protected int totalResults;


    public AvroRemoteQuery(RemoteCacheImpl cache, String jpqlString, long startOffset, int maxResults) {
        this.cache = cache;
        this.jpqlString = jpqlString;
        this.startOffset = startOffset;
        this.maxResults = maxResults;

    }

    public RemoteCacheImpl getCache() {
        return cache;
    }

    public String getJpqlString() {
        return jpqlString;
    }

    public long getStartOffset() {
        return startOffset;
    }

    public int getMaxResults() {
        return maxResults;
    }

    protected List<Object> executeQuery() {

        List<Object> results;
        AvroQueryOperation op = cache.getOperationsFactory().newAvroQueryOperation(this);
        Response response = op.execute();
        results = new ArrayList<>(response.getResults().size());
        for (ByteBuffer byteBuffer : response.getResults()) {
            try {
                results.add(cache.getRemoteCacheManager().getMarshaller().objectFromByteBuffer(byteBuffer.array()));
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return results;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> list() {
        if (results == null) {
            results = executeQuery();
        }

        return (List<T>) results;
    }

    @Override
    public int getResultSize() {
        list();
        return totalResults;
    }

}
