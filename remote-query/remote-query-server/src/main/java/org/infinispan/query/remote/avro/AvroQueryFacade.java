package org.infinispan.query.remote.avro;


import org.apache.avro.generic.GenericData;
import org.hibernate.hql.QueryParser;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.hql.lucene.LuceneProcessingChain;
import org.hibernate.hql.lucene.LuceneQueryParsingResult;
import org.hibernate.search.spi.SearchFactoryIntegrator;
import org.infinispan.AdvancedCache;
import org.infinispan.commons.CacheException;
import org.infinispan.objectfilter.Matcher;
import org.infinispan.objectfilter.impl.ReflectionMatcher;
import org.infinispan.query.CacheQuery;
import org.infinispan.query.Search;
import org.infinispan.query.SearchManager;
import org.infinispan.query.backend.QueryInterceptor;
import org.infinispan.query.dsl.embedded.impl.EmbeddedQuery;
import org.infinispan.query.impl.ComponentRegistryUtils;
import org.infinispan.query.remote.client.avro.AvroMarshaller;
import org.infinispan.query.remote.client.avro.Request;
import org.infinispan.query.remote.client.avro.Response;
import org.infinispan.server.core.QueryFacade;

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
public class AvroQueryFacade implements QueryFacade {

    private AvroMarshaller<Request> requestAvroMarshaller= new AvroMarshaller<>(Request.class);
    private AvroMarshaller<Response> responseAvroMarshaller = new AvroMarshaller<>(Response.class);
    private GenericRecordExternalizer externalizer = new GenericRecordExternalizer();

    @Override
    public byte[] query(AdvancedCache<byte[], byte[]> cache, byte[] query) {
        try {
            Request request = (Request) requestAvroMarshaller.objectFromByteBuffer(query);
            Response response = null;
            if (cache.getCacheConfiguration().indexing().enabled()) {
                try {
                    response = executeQuery(cache, request);
                } catch (IOException e) {
                    throw new CacheException("An exception has occurred during query execution", e);
                }
            }

            try {
                response = executeNonIndexedQuery(cache, request);
            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                throw new CacheException("An exception has occurred during query execution", e);
            }

            return responseAvroMarshaller.objectToByteBuffer(response);

        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            throw new CacheException("An exception has occurred during query execution", e);
        }

    }

    private Response executeNonIndexedQuery(AdvancedCache<byte[], byte[]> cache, Request request) throws IOException, ClassNotFoundException, InterruptedException {
        Class<? extends Matcher> matcherImplClass = ReflectionMatcher.class;
        EmbeddedQuery eq = new EmbeddedQuery(cache, request.getJpqlString().toString(), matcherImplClass);
        List<?> list = eq.list();
        int projSize = 0;
        if (eq.getProjection() != null && eq.getProjection().length > 0) {
            projSize = eq.getProjection().length;
        }
        List<ByteBuffer> results = new ArrayList<>(projSize == 0 ? list.size() : list.size() * projSize);
        byte[] buf;
        for (Object o: list) {
            if (projSize == 0) {
                buf = externalizer.objectToByteBuffer(o);
                ByteBuffer bb = ByteBuffer.allocate(buf.length);
                bb.put(buf);
                results.add(bb);
            } else {
                Object[] row = (Object[]) o;
                for (int j = 0; j < projSize; j++) {
                    buf = externalizer.objectToByteBuffer(o);
                    ByteBuffer bb = ByteBuffer.allocate(buf.length);
                    bb.put(buf);
                    results.add(bb);
                }
            }
        }

        Response response = new Response();
        response.setTotalResults(list.size());
        response.setNumResults(list.size());
        response.setProjectionSize(projSize);
        response.setResults(results);
        return response;
    }

    private Response executeQuery(AdvancedCache<byte[],byte[]> cache, Request request) throws IOException, ClassNotFoundException, InterruptedException {
        SearchManager searchManager = Search.getSearchManager(cache);
        CacheQuery cacheQuery;
        LuceneQueryParsingResult parsingResult;

        QueryParser queryParser = new QueryParser();
        SearchFactoryIntegrator searchFactory = (SearchFactoryIntegrator) searchManager.getSearchFactory();
        if (cache.getCacheConfiguration().compatibility().enabled()) {
            final QueryInterceptor queryInterceptor = ComponentRegistryUtils.getQueryInterceptor(cache);
            EntityNamesResolver entityNamesResolver = new EntityNamesResolver() {
                @Override
                public Class<?> getClassFromName(String entityName) {
                    return queryInterceptor.isIndexed(GenericData.Record.class) ? GenericData.Record.class: null;
                }
            };

            LuceneProcessingChain processingChain = new LuceneProcessingChain.Builder(searchFactory, entityNamesResolver)
                    .buildProcessingChainForClassBasedEntities();

            parsingResult = queryParser.parseQuery(request.getJpqlString().toString(), processingChain);
            cacheQuery = searchManager.getQuery(parsingResult.getQuery(), parsingResult.getTargetEntity());
        } else {
            throw new RuntimeException("NIY");
        }

        if (parsingResult.getSort() != null) {
            cacheQuery = cacheQuery.sort(parsingResult.getSort());
        }

        int projSize = 0;
        if (parsingResult.getProjections() != null && !parsingResult.getProjections().isEmpty()) {
            projSize = parsingResult.getProjections().size();
            cacheQuery = cacheQuery.projection(parsingResult.getProjections().toArray(new String[projSize]));
        }
        if (request.getStartOffset() > 0) {
            cacheQuery = cacheQuery.firstResult(request.getStartOffset());
        }
        if (request.getMaxResult() > 0) {
            cacheQuery = cacheQuery.maxResults(request.getMaxResult());
        }

        List<?> list = cacheQuery.list();
        List<ByteBuffer> results = new ArrayList<>(projSize == 0 ? list.size() : list.size() * projSize);
        byte[] buf;
        for (Object o: list) {
            if (projSize == 0) {
                buf = externalizer.objectToByteBuffer(o);
                ByteBuffer bb = ByteBuffer.allocate(buf.length);
                bb.put(buf);
                results.add(bb);
            } else {
                Object[] row = (Object[]) o;
                for (int j = 0; j < projSize; j++) {
                    buf = externalizer.objectToByteBuffer(o);
                    ByteBuffer bb = ByteBuffer.allocate(buf.length);
                    bb.put(buf);
                    results.add(bb);
                }
            }
        }

        Response response = new Response();
        response.setTotalResults(list.size());
        response.setNumResults(list.size());
        response.setProjectionSize(projSize);
        response.setResults(results);
        return response;

    }

}
