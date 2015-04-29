package org.infinispan.notifications.cachelistener.filter;

import org.infinispan.metadata.Metadata;

import java.io.Serializable;

/**
 * @author Pierre Sutra
 */
public class PostCacheEventFilterConverter <K, V, Object> extends AbstractCacheEventFilterConverter<K,V,Object>
      implements Serializable {
   
   @Override
   public Boolean filterAndConvert(java.lang.Object key, java.lang.Object oldValue,
         Metadata oldMetadata, java.lang.Object newValue,
         Metadata newMetadata, EventType eventType) {
      if (eventType.isPreEvent())
         return null;
      return Boolean.TRUE;
   }
}
