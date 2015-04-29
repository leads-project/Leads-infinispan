package org.infinispan.atomic.filter;

import org.infinispan.notifications.cachelistener.filter.CacheEventConverter;
import org.infinispan.notifications.cachelistener.filter.CacheEventConverterFactory;
import org.infinispan.notifications.cachelistener.filter.CompositeCacheEventFilterConverter;
import org.infinispan.notifications.cachelistener.filter.NamedFactory;

/**
 * @author Pierre Sutra
 */
@NamedFactory(name ="org.infinispan.atomic.container.remote.ConverterFactory")
public class ConverterFactory implements CacheEventConverterFactory {

   public static final String FACTORY_NAME = "org.infinispan.atomic.container.remote.ConverterFactory";

   @Override
   public <K,V,C> CacheEventConverter<K,V,C> getConverter(Object[] params) {

      return new CompositeCacheEventFilterConverter(
            new KeyBasedFilterConverter<>(new Object[] { params[0], params[1] }),
            new NonNullIdempotencyFilterConverter<>(new Object[] { params[0] }),
            new ObjectFilterConverter<>(params));
   }
   
}
