package org.apache.versioning;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.*;

/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public class EntryVersionShardedTreeMap<IncrementableEntryVersion,V>
        implements Serializable, SortedMap<IncrementableEntryVersion, V> {

    private static int THRESHOLD = 100;

    private Set<IncrementableEntryVersion> entries;
    private volatile SortedMap<IncrementableEntryVersion,EntryVersionTreeMap> delegate;

    public EntryVersionShardedTreeMap(){
        entries = new HashSet<IncrementableEntryVersion>();
        delegate = new TreeMap<IncrementableEntryVersion, EntryVersionTreeMap>();
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
        return (IncrementableEntryVersion) delegate.get(delegate.firstKey()).firstKey();
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
        return delegate.isEmpty();
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

    public Object readResolve()
            throws ObjectStreamException {
        delegate = new TreeMap<IncrementableEntryVersion, EntryVersionTreeMap>();
//        for(IncrementableEntryVersion version : entries){
//        }
        return null;
    }

}
