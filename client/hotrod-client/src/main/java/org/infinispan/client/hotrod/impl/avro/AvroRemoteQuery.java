package org.infinispan.client.hotrod.impl.avro;

import org.infinispan.client.hotrod.impl.RemoteCacheImpl;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.remote.client.avro.Response;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Pierre Sutra
 * @since 7.0
 */
public class AvroRemoteQuery implements Query, Cloneable {

   protected RemoteCacheImpl cache;
   protected String jpqlString;
   protected long startOffset; //todo can this really be long or it has to be int due to limitations in query module?
   protected InetSocketAddress location;
   protected List results;
   protected int numResults;
   protected int maxResults;

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
   
   public void setLocation(InetSocketAddress dest){
      this.location = dest;
   }
   
   public InetSocketAddress getLocation(){
      return this.location;
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
      numResults = response.getNumResults();
      return results;
   }

   @Override
   @SuppressWarnings("unchecked")
   public synchronized <T> List<T> list() {
      if (results == null) {
         results = executeQuery();
      }

      return (List<T>) results;
   }

   @Override
   public int getResultSize() {
      list();
      return numResults;
   }
   
   @Override
   public String toString(){
      return jpqlString + "(max="+maxResults+", offset="+startOffset+")";
   }
   
   @Override
   public Object clone() throws CloneNotSupportedException {
      AvroRemoteQuery query = (AvroRemoteQuery) super.clone();
      query.cache = this.cache;
      query.jpqlString = this.jpqlString;
      query.startOffset = this.startOffset;
      query.location = this.location;
      query.results = this.results;
      query.numResults = this.numResults;
      query.maxResults = this.maxResults;
      return query;
   }

}
