package org.infinispan.versioning;

import org.infinispan.commons.api.BasicCache;
import org.infinispan.versioning.utils.version.Version;

import java.rmi.RemoteException;
import java.util.Collection;

/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public interface VersionedCache<K,V> extends BasicCache<K,V> {

    void put(K key, V value, Version version);
    // do the same for *Async(), putAll(), putIfAbsent(), replace()

    Collection<V> get(K key, Version first, Version last);
    V get(K key, Version version);
    // do the same for *Async(), evict(), remove()

    @Override
    V put(K key, V value);

    @Override
    V get(Object k);

    V getLatest(K key, Version upperBound);
    V getEarliest(K key, Version lowerBound);

    Version getLatestVersion(K key);
    Version getLatestVersion(K key, Version upperBound);
    Version getEarliestVersion(K key);
    Version getEarliestVersion(K key, Version lowerBound);

}
