package org.infinispan.versioning.impl;

import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.infinispan.Cache;
import org.infinispan.container.versioning.IncrementableEntryVersion;
import org.infinispan.container.versioning.VersionGenerator;
import org.infinispan.versioning.utils.version.IncrementableEntryVersionComparator;

/**
 * Implement the Naive multi-versioning technique. In this implementation, all
 * the versions are stored under the same key. This implies there is one cache
 * per key.
 * 
 * @author valerio.schiavoni@gmail.com
 * 
 * @param <K>
 *            the type of the key
 * @param <V>
 *            the type of the value
 */
public class VersionedCacheNaiveImpl<K, V> extends
		VersionedCacheAbstractImpl<K, V> {

	/**
	 * 
	 * @param delegate
	 *            the super class
	 * @param generator
	 *            the version generator
	 * @param name
	 *            the name of the cache
	 */
	public VersionedCacheNaiveImpl(Cache<K, ?> delegate,
			VersionGenerator generator, String name) {
		super(delegate, generator, name);
	}

	/**
	 * Check if a cache with the given name exists.
	 */
	@Override
	public boolean containsKey(Object key) {
		return delegate.getCacheManager().cacheExists((String) key);
	}

	@Override
	public boolean containsValue(Object arg0) {
		throw new UnsupportedOperationException("to be implemented");
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	public Set<K> keySet() {
		return delegate.keySet();
	}

	/**
	 * Get the cache associated with the given key. This returns all the
	 * versions of this object. This operation is potentially very expensive.
	 */
	@Override
	protected SortedMap versionMapGet(K key) {
		
		TreeMap map = new TreeMap<IncrementableEntryVersion, V>(
				new IncrementableEntryVersionComparator());
		map.putAll(delegate.getCacheManager().getCache((String)key));
		return map;
	}

	@Override
	protected void versionMapPut(K key, V value,
			IncrementableEntryVersion version) {

		delegate.getCacheManager().getCache((String)key).put(key, version);

	}

}
