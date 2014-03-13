package org.infinispan.versioning;

import org.apache.commons.lang.NotImplementedException;

/**
 * A factory of {@link VersionedCache} instances.
 * 
 * @author valerio.schiavoni@gmail.com
 *
 */
public class VersionedCacheFactory<K,V> {
	public VersionedCache<K,V> newVersionedCache(K k, V v) {
		throw new NotImplementedException();
	}
}
