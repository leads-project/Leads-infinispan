package org.infinispan.client.hotrod.impl.avro;

import org.infinispan.client.hotrod.impl.RemoteCacheImpl;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.remote.client.avro.Response;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
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
        totalResults = (int) response.getTotalResults();
        if (response.getProjectionSize() > 0) {
            results = new ArrayList<Object>(response.getResults().size() / response.getProjectionSize());
            Iterator<ByteBuffer> it = response.getResults().iterator();
            while (it.hasNext()) {
                Object[] row = new Object[response.getProjectionSize()];
                for (int i = 0; i < response.getProjectionSize(); i++) {
                    try {
                        row[i] = cache.getRemoteCacheManager().getMarshaller().objectFromByteBuffer(it.next().array());
                    } catch (IOException e) {
                        e.printStackTrace();  // TODO: Customise this generated block
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();  // TODO: Customise this generated block
                    }
                }
                results.add(row);
            }
        }else {
                results = new ArrayList<Object>(response.getResults().size());
                for (ByteBuffer byteBuffer : response.getResults()) {
                    try {
                        results.add(cache.getRemoteCacheManager().getMarshaller().objectFromByteBuffer(byteBuffer.array()));
                    } catch (IOException e) {
                        e.printStackTrace();  // TODO: Customise this generated block
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();  // TODO: Customise this generated block
                    }
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
