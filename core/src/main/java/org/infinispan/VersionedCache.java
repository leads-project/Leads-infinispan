package org.infinispan;

import org.infinispan.commons.api.BasicCache;
import org.infinispan.container.versioning.IncrementableEntryVersion;

import java.util.Collection;

/**
 * // TODO: Document this
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public interface VersionedCache<K,V> extends BasicCache<K,V> {

    void put(K key, V value, IncrementableEntryVersion version);
    // do the same for *Async(), putAll(), putIfAbsent(), replace()

    Collection<V> get(K key, IncrementableEntryVersion first, IncrementableEntryVersion last);
    V get(K key, IncrementableEntryVersion version);
    // do the same for *Async(), evict(), remove()

    V getLatest(K key, IncrementableEntryVersion upperBound);
    V getEarliest(K key, IncrementableEntryVersion lowerBound);

    IncrementableEntryVersion getLatestVersion(K key);
    IncrementableEntryVersion getLatestVersion(K key, IncrementableEntryVersion upperBound);
    IncrementableEntryVersion getEarliestVersion(K key);
    IncrementableEntryVersion getEarliestVersion(K key, IncrementableEntryVersion lowerBound);

}
