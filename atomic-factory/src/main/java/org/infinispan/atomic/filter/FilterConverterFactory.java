package org.infinispan.atomic.filter;

import org.infinispan.notifications.cachelistener.filter.CacheEventFilterConverter;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilterConverterFactory;
import org.infinispan.notifications.cachelistener.filter.NamedFactory;

/**
 * @author Pierre Sutra
 */
@NamedFactory(name ="org.infinispan.atomic.filter.CacheEventFilterFactory")
public class FilterConverterFactory implements CacheEventFilterConverterFactory {

   public static final String FACTORY_NAME = "org.infinispan.atomic.filter.CacheEventFilterFactory";

   @Override 
   public CacheEventFilterConverter getFilterConverter(Object[] params) {
      
      return new CompositeCacheEventFilterConverter<>(
            new KeyBasedFilterConverter<>(new Object[] { params[0], params[1] }),
            new ContainerBasedCacheEventFilterConverter<>(new Object[] { params[0] }),
            ObjectFilterConverter.retrieve(params));
   }
}
