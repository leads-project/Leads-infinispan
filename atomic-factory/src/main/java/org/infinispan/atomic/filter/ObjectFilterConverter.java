package org.infinispan.atomic.filter;

import org.infinispan.Cache;
import org.infinispan.atomic.object.*;
import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.cachelistener.filter.AbstractCacheEventFilterConverter;
import org.infinispan.notifications.cachelistener.filter.CacheAware;
import org.infinispan.notifications.cachelistener.filter.EventType;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static org.infinispan.atomic.object.Utils.*;
import static org.jgroups.util.Util.*;

/**
 * @author Pierre Sutra
 * @since 7.2
 */
public class ObjectFilterConverter<K> extends AbstractCacheEventFilterConverter<K,Object,Object>
      implements CacheAware, Serializable{

   // Class fields

   private static Log log = LogFactory.getLog(ObjectFilterConverter.class);

   // Object fields

   private transient Cache<Object,Object> cache;
   private K key;
   private Object object;
   private Class clazz;
   private UUID client;
   private ArrayList<CallInvoke> pendingCalls;
   private CallOpen pendingOpenCall;
   private Object[] initArgs;
   private boolean forceNew;
   private Set<UUID> clients; 
   
   public ObjectFilterConverter(Object[] params){
      this(
            (UUID) params[0],
            (K) params[1],
            (Class) params[2],
            (boolean) params[3],
            params.length >= 5 ?  
                  Arrays.copyOfRange(params, 4, params.length-1) : null
      );       
   }
   
   public ObjectFilterConverter(
         final UUID client,
         final K key,
         final Class clazz,
         final boolean forceNew,
         final Object... initArgs){
      this.key = key;
      this.clazz = clazz;
      this.client = client;
      this.forceNew = forceNew;
      this.initArgs = initArgs;
      this.pendingCalls = null;
      this.clients = new HashSet<>();
   }

   @Override
   public void setCache(Cache <Object,Object> cache) {
      this.cache = cache;
   }

   @Override
   public Object filterAndConvert(Object key, Object oldValue, Metadata oldMetadata, Object newValue,
         Metadata newMetadata, EventType eventType) {

      assertNotNull(cache);
      
      assertEquals(key,this.key);

      try {

         Call call = (Call) Utils.unmarshall(newValue);
         
         CallFuture ret = new CallFuture(call.getCallID());

         log.debug(this + "Call " + call + " received");

         if (call instanceof CallInvoke) {

            if (object != null) {
      
               ret.set(handleInvocation((CallInvoke) call));

            } else if (pendingCalls != null) {

               log.debug(this + "Adding to pending calls");
               pendingCalls.add((CallInvoke) call);

            }

         } else if (call instanceof CallPersist) {

            log.debug(this + "Persistent state received");

            if (call.getCallerID().equals(client)){

               ret = new CallFuture(((CallPersist) call).getInitialCallID());
               ret.set(null);
               
            }else if (pendingOpenCall !=null 
                  && ((CallPersist)call).getInitialCallID().equals(pendingOpenCall.getCallID())) {

               if (object!=null) {
                  System.out.println("");
               }
               
               assertTrue(object==null);
               log.debug(this + "Updating state");
               object = ((CallPersist) call).getObject();
               assertTrue(object != null);
               
               log.debug(this + "Applying pending calls");
               for (CallInvoke invocation : pendingCalls)
                  handleInvocation(invocation);               
               
               pendingCalls = null;
               pendingOpenCall = null;
               
               ret = new CallFuture(((CallPersist) call).getInitialCallID());
               ret.set(null);

            }

         } else if (call instanceof CallOpen ) {
            
            clients.add(call.getCallerID());
            
            if (call.getCallerID().equals(client)) {

               assertTrue(object == null);
               assertTrue(pendingOpenCall == null);
               assertTrue(pendingCalls == null);

               if (forceNew || oldValue == null) {

                  log.debug(this + "Creating new object");
                  object = initObject(clazz, initArgs);
                  ret.set(null);

               } else {

                  Object previousCall = Utils.unmarshall(oldValue);

                  if (previousCall instanceof CallPersist 
                        && ((CallPersist)previousCall).getNclients() == 0) {

                     log.debug(this + "Retrieving object from persistent state.");
                     object = ((CallPersist) previousCall).getObject();
                     ret.set(null);
                     
                  } else {

                     log.debug(this + "Waiting for persistent state");
                     pendingOpenCall = (CallOpen) call;
                     pendingCalls = new ArrayList<>();

                  }

               }

            } else {

               if (object != null) {

                  log.debug(this + "Sending persistent state");
                  cache.putAsync(
                        key,
                        marshall(new CallPersist(client, call.getCallID(), clients.size(), object)));

               }
            }

         } else if (call instanceof CallClose) {

            clients.remove(call.getCallerID());
            
            if (call.getCallerID().equals(client)) {

               if (object != null) {

                  log.debug(this + "Persisting object");
                  cache.putAsync(
                        key,
                        marshall(new CallPersist(client, call.getCallID(), clients.size(), object)));

               } else {

                  throw new IllegalStateException("Closing while having no state.");

               }
               
            }

         }

         log.debug(this + "Future " + ret.getCallID() + " -> "+ret.isDone());
         
         if (ret.isDone())
            return marshall(ret);

      } catch (Exception e) {
         e.printStackTrace();
      }

      return null;
      
   }

   @Override
   public String toString(){
      return "ObjectFilterConverter["+ client.toString()+"]";
   }

   /**
    *
    * @param invocation
    * @return true if the operation is local
    * @throws InvocationTargetException
    * @throws IllegalAccessException
    */
   private Object handleInvocation(CallInvoke invocation)
         throws InvocationTargetException, IllegalAccessException {
      Object ret = callObject(object, invocation.method, invocation.arguments);
      log.debug(this+"Calling " + invocation+" (="+(ret==null ? "null" : ret.toString())+")");
      return  ret;
   }

}
