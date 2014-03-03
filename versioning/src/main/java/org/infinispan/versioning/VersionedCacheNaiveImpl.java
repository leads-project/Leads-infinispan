package org.infinispan.versioning;

import java.util.Set;
import java.util.SortedMap;

import org.infinispan.Cache;
import org.infinispan.container.versioning.IncrementableEntryVersion;
import org.infinispan.container.versioning.VersionGenerator;

public class VersionedCacheNaiveImpl<K,V> extends VersionedCacheImpl<K,V> {

	
	public VersionedCacheNaiveImpl(Cache<K, ?> delegate,
			VersionGenerator generator, String name) {
		super(delegate, generator, name);
	}

	@Override
	public boolean containsKey(Object arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsValue(Object arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	public Set keySet() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected SortedMap versionMapGet(Object key) {
		throw new UnsupportedOperationException();

	}

	@Override
	protected void versionMapPut(Object key, Object value,
			IncrementableEntryVersion verrsion) {
		throw new UnsupportedOperationException();

	}

}
