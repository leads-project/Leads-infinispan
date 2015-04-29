package org.infinispan.atomic.filter;

import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.cachelistener.filter.AbstractCacheEventFilterConverter;
import org.infinispan.notifications.cachelistener.filter.EventType;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.io.Serializable;
import java.util.UUID;

import static org.jgroups.util.Util.assertEquals;

/**
 * @author Pierre Sutra
 */
public class KeyBasedFilterConverter<K,V> extends AbstractCacheEventFilterConverter<K,V,Object> implements Serializable {

   private static Log log = LogFactory.getLog(KeyBasedFilterConverter.class);
   
   private K key;
   private UUID listenerID;
   
   public KeyBasedFilterConverter(Object[] parameters){
      assertEquals(2,parameters.length);
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
         log.debug(this+"Wrong key "+this.key+" vs "+key);
         return null;
      }

      return ret;

   }
}
