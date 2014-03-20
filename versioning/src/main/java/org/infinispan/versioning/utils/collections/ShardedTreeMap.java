package org.infinispan.versioning.utils.collections;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.*;

/**
 *
 * This class helps implementing the VersionedCacheAtomicShardedTreeMapImpl class.
 * It consists in a tree of subtrees, the first level is called entries, and it contains
 * the keys of the second level, named delegate.
 *
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public class ShardedTreeMap<K,V> implements Serializable, SortedMap<K, V>
{

    private final static int DEFAULT_THRESHOLD = 100;

    private Set<K> entries;
    private transient SortedMap<K,TreeMap<K,V>> delegate;
    private int threshhold; // how many entries are stored before creating a new subtree.

    public ShardedTreeMap(){
        entries = new HashSet<K>();
        delegate = new TreeMap<K, TreeMap<K,V>>();
        threshhold = DEFAULT_THRESHOLD;
    }

    public ShardedTreeMap(Integer threshhold){
        entries = new HashSet<K>();
        delegate = new TreeMap<K, TreeMap<K,V>>();
        this.threshhold = threshhold;
    }

    @Override
    public SortedMap<K, V> subMap(
            K v1,
            K v2) {
        SortedMap<K,V> result = new TreeMap<K, V>();
        for(K version : delegate.subMap(v1, v2).keySet()){
            result.putAll(delegate.get(version).subMap(v1, v2));
        }
        return result;
    }

    @Override
    public SortedMap<K, V> headMap(K v) {
        SortedMap<K,V> result = new TreeMap<K, V>();
        for(K version : delegate.headMap(v).keySet()){
            result.putAll(delegate.get(version).headMap(v));
        }
        return result;
    }

    @Override
    public SortedMap<K, V> tailMap(K v) {
        SortedMap<K,V> result = new TreeMap<K, V>();
        for(K version : delegate.tailMap(v).keySet()){
            result.putAll(delegate.get(version).tailMap(v));
        }
        return result;
    }

    @Override
    public K firstKey() {
        return delegate.get(delegate.firstKey()).firstKey();
    }

    @Override
    public K lastKey() {
        return delegate.get(delegate.lastKey()).lastKey();
    }

    @Override
    public int size() {
        int ret = 0;
        for(K v: delegate.keySet())
            ret+=delegate.get(v).size();
        return ret;
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public V get(Object o) {
        SortedMap<K,TreeMap<K,V>> m
                = delegate.tailMap((K)o);
        if(m.isEmpty())
            return null;
        return delegate.get(m.firstKey()).get(o);
    }

    @Override
    public V put(K k, V v) {
        V ret = get(k);
        if( delegate.isEmpty()
            || delegate.get(delegate.lastKey()).size()==threshhold ){
            delegate.put(k, new TreeMap());
        }
        K k1 = delegate.lastKey(); // FIXME
        TreeMap<K,V> inner = delegate.get(k1);
        inner.put(k, v);
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
        delegate = new TreeMap<K, TreeMap<K,V>>();
        for(K k: entries){
            TreeMap map = new TreeMap();
            delegate.put(k,map);
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
    public void putAll(Map<? extends K, ? extends V> map) {
        throw new UnsupportedOperationException("to be implemented");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("to be implemented");
    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException("to be implemented");
    }

    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException("to be implemented");
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
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

    @Override
    public Comparator<? super K> comparator() {
        throw new UnsupportedOperationException("to be implemented");
    }


}
