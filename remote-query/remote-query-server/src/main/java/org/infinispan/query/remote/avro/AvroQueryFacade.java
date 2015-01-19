package org.infinispan.query.remote.avro;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.lucene.search.Query;
import org.hibernate.hql.QueryParser;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.hql.lucene.LuceneProcessingChain;
import org.hibernate.hql.lucene.LuceneQueryParsingResult;
import org.hibernate.hql.lucene.spi.FieldBridgeProvider;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.spi.SearchFactoryIntegrator;
import org.infinispan.AdvancedCache;
import org.infinispan.query.CacheQuery;
import org.infinispan.query.Search;
import org.infinispan.query.SearchManager;
import org.infinispan.query.remote.client.avro.AvroSpecificMarshaller;
import org.infinispan.query.remote.client.avro.Request;
import org.infinispan.query.remote.client.avro.Response;
import org.infinispan.query.remote.logging.Log;
import org.infinispan.server.core.QueryFacade;
import org.infinispan.util.logging.LogFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Pierre Sutra
 * @since 7.0
 */
public class AvroQueryFacade implements QueryFacade {

   private static final Log log = LogFactory.getLog(AvroQueryFacade.class, Log.class);

   private AvroSpecificMarshaller<Request> requestAvroMarshaller= new AvroSpecificMarshaller<>(Request.class);
   private AvroSpecificMarshaller<Response> responseAvroMarshaller = new AvroSpecificMarshaller<>(Response.class);

   private AvroExternalizer genericAvroMarshaller = new AvroExternalizer();

   @Override
   public byte[] query(AdvancedCache<byte[], byte[]> cache, byte[] query) {

      Response response = new Response();

      try {

         Request request = (Request) requestAvroMarshaller.objectFromByteBuffer(query);
         log.debug(request.toString());

         SearchManager sm = Search.getSearchManager(cache);

         SearchFactoryIntegrator searchFactory = sm.getSearchFactory();
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

         LuceneQueryParsingResult parsingResult = qp.parseQuery(request.getJpqlString().toString(), processingChain);
         Query q = parsingResult.getQuery();
         CacheQuery cacheQuery;
         cacheQuery = sm.getClusteredQuery(q,GenericData.Record.class);
         if (request.getMaxResult() > 0)
            cacheQuery = cacheQuery.maxResults(request.getMaxResult());
         if (parsingResult.getSort() != null)
            cacheQuery = cacheQuery.sort(parsingResult.getSort());
         if (request.getStartOffset() >= 0)
            cacheQuery = cacheQuery.firstResult(request.getStartOffset().intValue());

         List<Object> list = cacheQuery.list();
            
         List<ByteBuffer> results = new ArrayList<>();
         if (parsingResult.getProjections().size()==0){
            for (Object o: list) {
               assert o!=null;
               results.add(ByteBuffer.wrap((byte[])o));
            }
         }else{
            for (Object o: list) {
               GenericData.Record record = (GenericData.Record) genericAvroMarshaller.objectFromByteBuffer((byte[]) o);
               GenericRecordBuilder builder = new GenericRecordBuilder(record.getSchema());
               GenericData.Record copy = builder.build();
               for(Schema.Field f : record.getSchema().getFields()){
                  if (parsingResult.getProjections().contains(f.name()))
                     copy.put(f.name(),record.get(f.name()));
               }
               results.add(ByteBuffer.wrap(genericAvroMarshaller.objectToByteBuffer(copy)));
            }
         }
      
         response.setNumResults(cacheQuery.getResultSize());
         response.setResults(results);

      } catch (Exception e) {
         e.printStackTrace();
         response.setNumResults(0);
         response.setResults(new ArrayList<ByteBuffer>());
      }

      try {
         return responseAvroMarshaller.objectToByteBuffer(response);
      } catch (IOException e) {
         e.printStackTrace();
      }
      
      return null;

   }

}
