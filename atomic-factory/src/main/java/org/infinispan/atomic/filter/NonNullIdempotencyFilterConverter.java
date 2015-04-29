package org.infinispan.atomic.filter;

import org.infinispan.atomic.object.Call;
import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.cachelistener.filter.AbstractCacheEventFilterConverter;
import org.infinispan.notifications.cachelistener.filter.EventType;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

import static org.infinispan.atomic.object.Utils.lruCache;

/**
 * @author Pierre Sutra
 */
public class NonNullIdempotencyFilterConverter<K,V> extends AbstractCacheEventFilterConverter<K,V,Object>
      implements Serializable{

   // Could be based on version numbers ?
   private static Log log = LogFactory.getLog(NonNullIdempotencyFilterConverter.class);
   private Map<UUID,Integer> received = lruCache(5000);
   private UUID listenerID;
   
   public NonNullIdempotencyFilterConverter(Object[] parameters){
      assert (parameters.length==1);
      listenerID = (UUID) parameters[0];
   }

   @Override
   public String toString(){
      return "NonNullCallIdempotencyFilterConverter["+listenerID+"]";
   }

   @Override 
   public Object filterAndConvert(Object key, Object oldValue, Metadata oldMetadata, Object newValue,
         Metadata newMetadata, EventType eventType) {
      
      if (newValue==null) {
         log.debug(this+"Null value");
         return null;
      }

      Call call = (Call) newValue;

      if (received.containsKey(call.getCallID())) {
         log.debug(this+"Already received "+call.getCallID());
         return null;
      }

      received.put(call.getCallID(),0);

      return true;
   }

}
