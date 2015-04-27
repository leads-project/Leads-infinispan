package org.infinispan.atomic.filter;

import org.infinispan.notifications.cachelistener.filter.*;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.jgroups.util.Util.assertTrue;

/**
* @author Pierre Sutra
* @since 7.2
*/
public class FilterConverterFactory implements CacheEventConverterFactory, CacheEventFilterFactory {

   public static final String FACTORY_NAME = "org.infinispan.atomic.container.remote.RemoteFilterConverterFactory";
   
   private ConcurrentHashMap<UUID,CompositeCacheEventFilterConverter> registered  = new ConcurrentHashMap<>();
   
   @Override
   public <K, V> CacheEventFilter<K, V> getFilter(Object[] params) {
      
      assertTrue(params.length>=4);
      UUID listenerID =(UUID) params[0];
      
      assertTrue(!registered.containsKey(listenerID));
      CompositeCacheEventFilterConverter filterConverter = 
            new CompositeCacheEventFilterConverter<>(
                  new PostCacheEventFilterConverter(),
                  new KeyBasedFilterConverter<>(new Object[] { params[0], params[1] }),
                  new NonNullIdempotencyFilterConverter<>(new Object[] { params[0] }),
                  new ObjectFilterConverter<>(params));
      
      registered.put(listenerID,filterConverter);
      
      return filterConverter;
   }

   @Override 
   public <K,V,C> CacheEventConverter<K,V,C> getConverter(Object[] params) {

      assertTrue(params.length==1);
      UUID listenerID =(UUID) params[0];
      assertTrue(registered.containsKey(listenerID));
      CompositeCacheEventFilterConverter filterConverter = registered.get(listenerID);
      registered.remove(listenerID);
      return filterConverter;
   }
}
