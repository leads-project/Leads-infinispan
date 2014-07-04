package org.infinispan.versioning;

import org.infinispan.commons.api.BasicCache;
import org.infinispan.versioning.utils.version.Version;

import java.util.Collection;
import java.util.Map;

/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public interface VersionedCache<K,V> extends BasicCache<K,V> {

    void put(K key, V value, Version version);
    // do the same for *Async(), putAll(), putIfAbsent(), replace()

    /**
     *
     * Return all the versions of key <i>k</i> from version <i><first/i> included to <i>last</i> excluded.
     *
     *
     * @param key
     * @param first
     * @param last
     * @return
     */
    Collection<Version> get(K key, Version first, Version last);

    /**
     *
     * Return the value of key <i>k</i> stored at verrsion <i>version</i> if it exists.
     * In case the version is missing, <i>null</i> is returned.
     *
     * @param key
     * @param version
     * @return
     */
    V get(K key, Version version);
    // do the same for *Async(), evict(), remove()

    @Override
    V put(K key, V value);

    void putAll(K key, Map<Version,V> map);

    @Override
    V get(Object k);

    V getLatest(K key, Version upperBound);
    V getEarliest(K key, Version lowerBound);

    Version getLatestVersion(K key);
    Version getLatestVersion(K key, Version upperBound);
    Version getEarliestVersion(K key);
    Version getEarliestVersion(K key, Version lowerBound);

}
