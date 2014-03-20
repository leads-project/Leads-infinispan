package org.infinispan.versioning.impl;

import org.infinispan.Cache;
import org.infinispan.versioning.utils.version.Version;
import org.infinispan.versioning.utils.version.VersionGenerator;

import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Implement the Naive multiversioning technique. In this implementation, all
 * the versions are stored under the same key. This implies there is one cache
 * per key.
 * 
 * @author valerio.schiavoni@gmail.com
 * 
 * @param <K>
 *            the type of the key
 * @param <V>
 *            the type of the version
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
	public VersionedCacheNaiveImpl(Cache delegate,VersionGenerator generator, String name) {
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
	protected SortedMap<Version,V> versionMapGet(K key) {
		TreeMap<Version,V> map = new TreeMap<Version, V>();
		map.putAll(delegate);
		return map;
	}
	
	@Override
	protected void versionMapPut(K key, V value, Version version) {
		delegate.put(version, value);

	}
	

}
