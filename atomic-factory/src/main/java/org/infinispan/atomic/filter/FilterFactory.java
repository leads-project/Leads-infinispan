package org.infinispan.atomic.filter;

import org.infinispan.notifications.cachelistener.filter.*;

/**
* @author Pierre Sutra
* @since 7.2
*/

@NamedFactory(name ="org.infinispan.atomic.container.remote.FilterFactory")
public class FilterFactory implements CacheEventFilterFactory {

   public static final String FACTORY_NAME = "org.infinispan.atomic.container.remote.FilterFactory";

   @Override
   public <K, V> CacheEventFilter<K, V> getFilter(Object[] params) {
      return new PostCacheEventFilter<>();
   }
}
