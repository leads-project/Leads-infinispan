package org.infinispan.atomic.filter;

import org.infinispan.notifications.cachelistener.filter.*;

/**
* @author Pierre Sutra
* @since 7.2
*/

@NamedFactory(name ="org.infinispan.atomic.container.remote.RemoteFilterConverterFactory")
public class FilterFactory implements CacheEventConverterFactory, CacheEventFilterFactory {

   public static final String FACTORY_NAME = "org.infinispan.atomic.container.remote.RemoteFilterConverterFactory";
   
   @Override
   public <K,V,C> CacheEventConverter<K,V,C> getConverter(Object[] params) {

      return new CompositeCacheEventFilterConverter(
                  new KeyBasedFilterConverter<>(new Object[] { params[0], params[1] }),
                  new NonNullIdempotencyFilterConverter<>(new Object[] { params[0] }),
                  new ObjectFilterConverter<>(params));
   }

   @Override
   public <K, V> CacheEventFilter<K, V> getFilter(Object[] params) {
      return new PostCacheEventFilter<>();
   }
}
