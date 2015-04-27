package org.infinispan.notifications.cachelistener.filter;

import org.infinispan.Cache;

/**
 * @author Pierre Sutra
 */
public interface CacheAware {
   
   public void setCache(Cache<Object,Object> cache);
   
}
