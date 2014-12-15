package org.infinispan.query.clustered;

import org.hibernate.search.exception.SearchException;
import org.infinispan.Cache;
import org.infinispan.commons.util.concurrent.NotifyingFutureImpl;
import org.infinispan.commons.util.concurrent.NotifyingNotifiableFuture;
import org.infinispan.remoting.responses.Response;
import org.infinispan.remoting.responses.SuccessfulResponse;
import org.infinispan.remoting.rpc.ResponseMode;
import org.infinispan.remoting.rpc.RpcManager;
import org.infinispan.remoting.rpc.RpcOptions;
import org.infinispan.remoting.transport.Address;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

/**
 * Invoke a ClusteredQueryCommand on the cluster, including on own node.
 * 
 * @author Israel Lacerra <israeldl@gmail.com>
 * @author Sanne Grinovero <sanne@infinispan.org> (C) 2011 Red Hat Inc.
 * @author Pierre Sutra
 * @since 5.1
 */
public class ClusteredQueryInvoker {

   private final RpcManager rpcManager;
   private final Cache<?, ?> localCacheInstance;
   private final Address myAddress;
   private final ExecutorService asyncExecutor;
   private final RpcOptions rpcOptions;

   ClusteredQueryInvoker(Cache<?, ?> localCacheInstance, ExecutorService asyncExecutor) {
      this.asyncExecutor = asyncExecutor;
      this.rpcManager = localCacheInstance.getAdvancedCache().getComponentRegistry().getLocalComponent(RpcManager.class);
      this.localCacheInstance = localCacheInstance;
      this.myAddress = rpcManager.getAddress();
      this.rpcOptions = rpcManager.getRpcOptionsBuilder(ResponseMode.SYNCHRONOUS)
            .timeout(10000, TimeUnit.MILLISECONDS).build();
   }

   /**
    * Retrieves the value (using doc index) in a remote query instance
    * 
    * @param doc
    *           Doc index of the value on remote query
    * @param address
    *           Address of the node who has the value
    * @param queryId
    *           Id of the query
    * @return The value of index doc of the query with queryId on node at address
    */
   public Object getValue(int doc, Address address, UUID queryId)
         throws SearchException {

      ClusteredQueryCommand clusteredQuery = ClusteredQueryCommand.retrieveKeyFromLazyQuery(
               localCacheInstance, queryId, doc);

      if (address.equals(myAddress)) {
         Future<QueryResponse> localResponse = localInvoke(clusteredQuery);
         try {
            return localResponse.get().getFetchedValue();
         } catch (InterruptedException e) {
            throw new SearchException("interrupted while searching locally", e);
         } catch (ExecutionException e) {
            throw new SearchException("Exception while searching locally", e);
         }
      } else {
         List<Address> addresss = new ArrayList<Address>(1);
         addresss.add(address);

         Map<Address, Response> responses = rpcManager.invokeRemotely(addresss, clusteredQuery, rpcOptions);
         List<QueryResponse> objects = cast(responses);
         return objects.get(0).getFetchedValue();
      }
   }

   /**
    * Broadcast this ClusteredQueryCommand to all cluster nodes. The command will be also invoked on
    * local node.
    * 
    * @param clusteredQuery
    * @return A list with all responses
    */
   public List<QueryResponse> broadcast(ClusteredQueryCommand clusteredQuery)
         throws SearchException {

      List<QueryResponse> ret;
      try {
         NotifyingNotifiableFuture<Map<Address, Response>> remoteFutures = new NotifyingFutureImpl<>();
         rpcManager.invokeRemotelyInFuture(remoteFutures, null, clusteredQuery, rpcOptions);
         Future<QueryResponse> localFuture = localInvoke(clusteredQuery);
         ret=cast(remoteFutures.get());
         ret.add(localFuture.get());
      } catch (InterruptedException | ExecutionException e1) {
         throw new SearchException("Exception while searching locally", e1);
      }

      return ret;
   }

   private Future<QueryResponse> localInvoke(ClusteredQueryCommand clusteredQuery) {
      ClusteredQueryCallable clusteredQueryCallable = new ClusteredQueryCallable(clusteredQuery,
               localCacheInstance);
      return asyncExecutor.submit(clusteredQueryCallable);
   }

   private List<QueryResponse> cast(Map<Address, Response> responses) {
      List<QueryResponse> objects = new LinkedList<QueryResponse>();
      for (Entry<Address, Response> pair : responses.entrySet()) {
         Response resp = pair.getValue();
         if (resp instanceof SuccessfulResponse) {
            QueryResponse response = (QueryResponse) ((SuccessfulResponse) resp).getResponseValue();
            objects.add(response);
         } else {
            throw new SearchException("Unexpected response: " + resp);
         }
      }

      return objects;
   }

   /**
    * Created to call a ClusteredQueryCommand on own node.
    * 
    * @author Israel Lacerra <israeldl@gmail.com>
    * @since 5.1
    */
   private static final class ClusteredQueryCallable implements Callable<QueryResponse> {

      private final ClusteredQueryCommand clusteredQuery;

      private final Cache<?, ?> localInstance;

      ClusteredQueryCallable(ClusteredQueryCommand clusteredQuery, Cache<?, ?> localInstance) {
         this.clusteredQuery = clusteredQuery;
         this.localInstance = localInstance;
      }

      @Override
      public QueryResponse call() throws Exception {
         try {
            return clusteredQuery.perform(localInstance);
         } catch (Throwable e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
         }
      }

   }

}
