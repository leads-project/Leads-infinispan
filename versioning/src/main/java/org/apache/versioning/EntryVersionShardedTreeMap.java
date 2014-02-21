package org.apache.versioning;

import java.io.Serializable;
import java.util.*;

/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public class EntryVersionShardedTreeMap<IncrementableEntryVersion,V> implements Serializable, SortedMap<IncrementableEntryVersion, V> {

    private SortedMap<IncrementableEntryVersion,TreeMap<IncrementableEntryVersion,V>> delegate;

    public EntryVersionShardedTreeMap(int THRESHOLD){
        delegate = new TreeMap<IncrementableEntryVersion, TreeMap<IncrementableEntryVersion, V>>();
    }

    @Override
    public Comparator<? super IncrementableEntryVersion> comparator() {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public SortedMap<IncrementableEntryVersion, V> subMap(
            IncrementableEntryVersion incrementableEntryVersion,
            IncrementableEntryVersion incrementableEntryVersion2) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public SortedMap<IncrementableEntryVersion, V> headMap(IncrementableEntryVersion incrementableEntryVersion) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public SortedMap<IncrementableEntryVersion, V> tailMap(IncrementableEntryVersion incrementableEntryVersion) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public IncrementableEntryVersion firstKey() {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public IncrementableEntryVersion lastKey() {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public int size() {
        return 0;  // TODO: Customise this generated block
    }

    @Override
    public boolean isEmpty() {
        return false;  // TODO: Customise this generated block
    }

    @Override
    public boolean containsKey(Object o) {
        return false;  // TODO: Customise this generated block
    }

    @Override
    public boolean containsValue(Object o) {
        return false;  // TODO: Customise this generated block
    }

    @Override
    public V get(Object o) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public V put(IncrementableEntryVersion incrementableEntryVersion, V v) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public V remove(Object o) {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public void putAll(Map<? extends IncrementableEntryVersion, ? extends V> map) {
        // TODO: Customise this generated block
    }

    @Override
    public void clear() {
        // TODO: Customise this generated block
    }

    @Override
    public Set<IncrementableEntryVersion> keySet() {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public Collection<V> values() {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public Set<Entry<IncrementableEntryVersion, V>> entrySet() {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public boolean equals(Object o) {
        return false;  // TODO: Customise this generated block
    }

    @Override
    public int hashCode() {
        return 0;  // TODO: Customise this generated block
    }
}
