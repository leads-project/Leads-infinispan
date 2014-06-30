package org.infinispan.query.remote.avro;


import org.apache.avro.generic.GenericData;
import org.apache.lucene.search.Query;
import org.hibernate.hql.QueryParser;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.hql.lucene.LuceneProcessingChain;
import org.hibernate.hql.lucene.spi.FieldBridgeProvider;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.spi.SearchFactoryIntegrator;
import org.infinispan.AdvancedCache;
import org.infinispan.query.Search;
import org.infinispan.query.SearchManager;
import org.infinispan.query.remote.client.avro.AvroMarshaller;
import org.infinispan.query.remote.client.avro.Request;
import org.infinispan.query.remote.client.avro.Response;
import org.infinispan.server.core.QueryFacade;

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
            SearchManager sm = Search.getSearchManager(cache);

            QueryParser queryParser = new QueryParser();
            SearchFactoryIntegrator searchFactory = (SearchFactoryIntegrator) sm.getSearchFactory();
            QueryBuilder qb = sm.buildQueryBuilderForClass(GenericData.Record.class).get();
            QueryParser qp = new QueryParser();

            EntityNamesResolver resolver = new EntityNamesResolver() {
                @Override
                public Class<?> getClassFromName(String s) {
                    if (s.equals(GenericData.Record.class.getName()))
                        return GenericData.Record.class;
                    return null;
                }
            };
            LuceneProcessingChain processingChain
                    = new LuceneProcessingChain.Builder(searchFactory,resolver).buildProcessingChainForDynamicEntities(
                    new FieldBridgeProvider() {
                        @Override
                        public FieldBridge getFieldBridge(String s, String s2) {
                            return new ValueWrapperFieldBridge();
                        }
                    });

            Query q = qp.parseQuery(request.getJpqlString().toString(),processingChain).getQuery();

            List<Object> list = sm.getQuery(q).list();
            List<ByteBuffer> results = new ArrayList<>();
            for (Object o: list) {
                results.add(ByteBuffer.wrap((byte[])o));
            }

            Response response = new Response();
            response.setTotalResults(list.size());
            response.setNumResults(list.size());
            response.setProjectionSize(0);
            response.setResults(results);
            return responseAvroMarshaller.objectToByteBuffer(response);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

}
