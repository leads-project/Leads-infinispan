package org.infinispan.atomic.filter;

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
public class KeyBasedFilterConverter<K,V> extends AbstractCacheEventFilterConverter<K,V,Object> implements Serializable {

   private static Log log = LogFactory.getLog(KeyBasedFilterConverter.class);
   
   private K key;
   private UUID listenerID;
   
   public KeyBasedFilterConverter(Object[] parameters){
      assert (parameters.length==2);
      listenerID = (UUID) parameters[0];
      key = (K) parameters[1];
   }

   @Override
   public String toString(){
      return "KeyBasedFilterConverter"+listenerID+"]";
   }

   @Override 
   public Object filterAndConvert(Object key, Object oldValue, Metadata oldMetadata, Object newValue,
         Metadata newMetadata, EventType eventType) {

      boolean ret = this.key.equals(key);

      if (!ret) {
         log.debug(this+"Wrong key");
         return null;
      }

      return ret;

   }
}
