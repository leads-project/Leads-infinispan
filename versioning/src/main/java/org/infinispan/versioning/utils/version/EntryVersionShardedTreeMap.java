package org.infinispan.versioning.utils.version;

import org.infinispan.atomic.AtomicObjectFactory;
import org.infinispan.manager.DefaultCacheManager;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.*;

/**
 *
 * This class helps implementing the VersionedCacheAtomicShardedTreeMapImpl class.
 * It consists in a tree of subtrees, the first level is called entries, and it contains
 * the keys of the second level, named delegate.
 *
 * TODO in the current implementation, everything is stored in the default cache.
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public class EntryVersionShardedTreeMap<IncrementableEntryVersion,V>
        implements Serializable, SortedMap<IncrementableEntryVersion, V> {

    private final static int THRESHOLD = 100; // this threshold indicates on many entries are stored before creating a new subtree
    private transient static AtomicObjectFactory factory = new AtomicObjectFactory((new DefaultCacheManager()).getCache());

    private Set<IncrementableEntryVersion> entries;
    private transient SortedMap<IncrementableEntryVersion,EntryVersionTreeMap<IncrementableEntryVersion,V>> delegate;

    public EntryVersionShardedTreeMap(){
        entries = new HashSet<IncrementableEntryVersion>();
        delegate = new TreeMap<IncrementableEntryVersion, EntryVersionTreeMap<IncrementableEntryVersion,V>>();
    }

    @Override
    public Comparator<? super IncrementableEntryVersion> comparator() {
        return (Comparator<? super IncrementableEntryVersion>) new IncrementableEntryVersionComparator();
    }

    @Override
    public SortedMap<IncrementableEntryVersion, V> subMap(
            IncrementableEntryVersion v1,
            IncrementableEntryVersion v2) {
        SortedMap<IncrementableEntryVersion,V> result = new TreeMap<IncrementableEntryVersion, V>();
        for(IncrementableEntryVersion version : delegate.subMap(v1, v2).keySet()){
            result.putAll(delegate.get(version).subMap(v1, v2));
        }
        return result;
    }

    @Override
    public SortedMap<IncrementableEntryVersion, V> headMap(IncrementableEntryVersion v) {
        SortedMap<IncrementableEntryVersion,V> result = new TreeMap<IncrementableEntryVersion, V>();
        for(IncrementableEntryVersion version : delegate.headMap(v).keySet()){
            result.putAll(delegate.get(version).headMap(v));
        }
        return result;
    }

    @Override
    public SortedMap<IncrementableEntryVersion, V> tailMap(IncrementableEntryVersion v) {
        SortedMap<IncrementableEntryVersion,V> result = new TreeMap<IncrementableEntryVersion, V>();
        for(IncrementableEntryVersion version : delegate.tailMap(v).keySet()){
            result.putAll(delegate.get(version).tailMap(v));
        }
        return result;
    }

    @Override
    public IncrementableEntryVersion firstKey() {
        return delegate.get(delegate.firstKey()).firstKey();
    }

    @Override
    public IncrementableEntryVersion lastKey() {
        return delegate.get(delegate.lastKey()).lastKey();
    }

    @Override
    public int size() {
        int ret = 0;
        for(IncrementableEntryVersion v: delegate.keySet())
            ret+=delegate.get(v).size();
        return ret;
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public V get(Object o) {
        SortedMap<IncrementableEntryVersion,EntryVersionTreeMap<IncrementableEntryVersion,V>> m
                = delegate.tailMap((IncrementableEntryVersion)o);
        if(m.isEmpty())
            return null;
        return delegate.get(m.firstKey()).get(o);
    }

    @Override
    public V put(IncrementableEntryVersion incrementableEntryVersion, V v) {
        V ret = get(incrementableEntryVersion);
        if(delegate.get(delegate.lastKey()).size()==THRESHOLD)
            delegate.put(incrementableEntryVersion,factory.getInstanceOf(EntryVersionTreeMap.class,incrementableEntryVersion,true));
        delegate.get(delegate.lastKey()).put(incrementableEntryVersion,v);
        return v;
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    // When the object is unserialized, we reconstruct all the subtrees.
    public Object readResolve() throws ObjectStreamException {
        delegate = new TreeMap<IncrementableEntryVersion, EntryVersionTreeMap<IncrementableEntryVersion,V>>();
        for(IncrementableEntryVersion version : entries){
            EntryVersionTreeMap map = factory.getInstanceOf(EntryVersionTreeMap.class,version.toString(),true,null,false);
            delegate.put(version,map);
        }
        return this;
    }

    //
    // NOT YET IMPLEMENTED
    //

    @Override
    public V remove(Object o) {
        throw new UnsupportedOperationException("to be implemented");
    }

    @Override
    public void putAll(Map<? extends IncrementableEntryVersion, ? extends V> map) {
        throw new UnsupportedOperationException("to be implemented");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("to be implemented");
    }

    @Override
    public Set<IncrementableEntryVersion> keySet() {
        throw new UnsupportedOperationException("to be implemented");
    }

    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException("to be implemented");
    }

    @Override
    public Set<Entry<IncrementableEntryVersion, V>> entrySet() {
        throw new UnsupportedOperationException("to be implemented");
    }

    @Override
    public boolean containsKey(Object o) {
        throw new UnsupportedOperationException("to be implemented");
    }

    @Override
    public boolean containsValue(Object o) {
        throw new UnsupportedOperationException("to be implemented");
    }

}
