package org.infinispan.versioning.impl;

import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;

import org.infinispan.Cache;
import org.infinispan.versioning.utils.version.Version;
import org.infinispan.versioning.utils.version.VersionGenerator;

public class VersionedCacheDummyImpl<K,V> extends VersionedCacheAbstractImpl<K,V> {

	public VersionedCacheDummyImpl(Cache cache,
			VersionGenerator generator, String cacheName) {
		super(cache, generator, cacheName);
	}

	@Override
	public boolean containsKey(Object arg0) {
		return false;
	}

	@Override
	public boolean containsValue(Object arg0) {
		return false;
	}

	@Override
	public boolean isEmpty() {
		return  true;
	}

	@Override
	public Set<K> keySet() {
		return new TreeSet<K>();
	}

	@Override
	protected SortedMap<Version, V> versionMapGet(K key) {		
		return null;
	}

	@Override
	protected void versionMapPut(K key, V value, Version version) {
		
	}

}
