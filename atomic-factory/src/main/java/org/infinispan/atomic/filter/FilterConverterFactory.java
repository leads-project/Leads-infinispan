package org.infinispan.atomic.filter;

import org.infinispan.notifications.cachelistener.filter.*;

/**
 * @author Pierre Sutra
 */
@NamedFactory(name ="org.infinispan.atomic.filter.CacheEventFilterFactory")
public class FilterConverterFactory implements CacheEventFilterConverterFactory {

   public static final String FACTORY_NAME = "org.infinispan.atomic.filter.CacheEventFilterFactory";

   @Override 
   public <K, V, C> CacheEventFilterConverter<K, V, C> getFilterConverter(Object[] params) {
      return (CacheEventFilterConverter) new CompositeCacheEventFilterConverter<>(
            new KeyBasedFilterConverter<>(new Object[] { params[0], params[1] }),
            new NonNullIdempotencyFilterConverter<>(new Object[] { params[0] }),
            new ObjectFilterConverter<>(params));
   }
}
