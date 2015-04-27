package org.infinispan.notifications.cachelistener.filter;

import org.infinispan.Cache;
import org.infinispan.metadata.Metadata;

import java.io.Serializable;

/**
 * @author Pierre Sutra
 * @since 7.2
 */
public class CompositeCacheEventFilterConverter<K,V,C> extends AbstractCacheEventFilterConverter<K, V,C> 
      implements Serializable, CacheAware{
   
   private final CacheEventFilterConverter<? super K, ? super V, ? extends C>[] filters;

   public CompositeCacheEventFilterConverter(CacheEventFilterConverter<? super K, ? super V, ? extends C>... filters) {
      this.filters = filters;
   }

   @Override 
   public C filterAndConvert(K key, V oldValue, Metadata oldMetadata, V newValue, Metadata newMetadata,
         EventType eventType) {

      C ret = null;
      
      for (CacheEventFilterConverter<? super K, ? super V, ? extends C> k : filters) {
         ret = k.filterAndConvert(key, oldValue, oldMetadata, newValue, newMetadata, eventType);
         if (ret==null) break;
      }
      
      return ret;

   }

   @Override 
   public void setCache(Cache<Object, Object> cache) {
      for (CacheEventFilterConverter<? super K, ? super V, ? extends C> k : filters) {
         if (k instanceof CacheAware)
            ((CacheAware) k).setCache(cache);
      }
   }
}
