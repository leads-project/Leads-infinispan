package org.infinispan.atomic;

import org.infinispan.notifications.cachelistener.filter.CacheEventFilterConverter;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilterConverterFactory;
import org.infinispan.notifications.cachelistener.filter.NamedFactory;

/**
* // TODO: Document this
*
* @author otrack
* @since 4.0
*/
@NamedFactory(name = "static-converter")
public class StaticConverterFactory implements CacheEventFilterConverterFactory {

   final CacheEventFilterConverter staticConverter = new StaticCacheEventConverter();

   @Override public <K, V, C> CacheEventFilterConverter<K, V, C> getFilterConverter(Object[] params) {
      return staticConverter;
   }
}
