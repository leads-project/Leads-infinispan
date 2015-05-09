package org.infinispan.atomic.filter;

import org.infinispan.atomic.object.Call;
import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.cachelistener.filter.AbstractCacheEventFilterConverter;
import org.infinispan.notifications.cachelistener.filter.EventType;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.io.Serializable;
import java.util.UUID;

/**
 * @author Pierre Sutra
 */
public class ListenerBasedCacheEventFilterConverter<K,V> extends AbstractCacheEventFilterConverter<K,V,Object>
      implements Serializable{

   private static Log log = LogFactory.getLog(ListenerBasedCacheEventFilterConverter.class);
   private UUID listenerID;
   
   public ListenerBasedCacheEventFilterConverter(Object[] parameters){
      assert (parameters.length==1);
      listenerID = (UUID) parameters[0];
   }

   @Override
   public String toString(){
      return "ListenerBasedCacheEventFilterConverter["+ listenerID +"]";
   }

   @Override 
   public Object filterAndConvert(Object key, Object oldValue, Metadata oldMetadata, Object newValue,
         Metadata newMetadata, EventType eventType) {
      
      if (newValue==null) {
         log.warn(this + "Null value received");
         return null;
      }
      
      Call call = (Call) newValue;
      
      if (!call.getListenerID().equals(listenerID)) {
         if (log.isDebugEnabled()) log.trace(this + "Wrong listener");
         return null;
      }

      return true;
   }

}
