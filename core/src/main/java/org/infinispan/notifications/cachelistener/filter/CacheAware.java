package org.infinispan.notifications.cachelistener.filter;

import org.infinispan.Cache;

/**
 * @author Pierre Sutra
 */
public interface CacheAware<K,V> {

   public void setCache(Cache<K,V> cache);

}