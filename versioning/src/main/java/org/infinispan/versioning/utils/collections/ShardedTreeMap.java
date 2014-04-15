package org.infinispan.versioning.utils.collections;

import org.infinispan.atomic.AtomicObjectFactory;
import org.infinispan.versioning.VersionedCacheFactory;

import java.io.IOException;
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

    private final static int DEFAULT_THRESHOLD = 1000 ;
    private static transient AtomicObjectFactory factory = AtomicObjectFactory.forCache(VersionedCacheFactory.cache);

    private SortedMap<K,TreeMap<K,V>> delegate;
    private Set<K> entries;
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
        for(K key : delegate.subMap(v1, v2).keySet()){
            allocateTree(key);
            result.putAll(delegate.get(key).subMap(v1, v2));
        }
        return result;
    }

    @Override
    public SortedMap<K, V> headMap(K v) {
        SortedMap<K,V> result = new TreeMap<K, V>();
        for(K key : delegate.headMap(v).keySet()){
            allocateTree(key);
            result.putAll(delegate.get(key).headMap(v));
        }
        return result;
    }

    @Override
    public SortedMap<K, V> tailMap(K v) {
        SortedMap<K,V> result = new TreeMap<K, V>();
        for(K key : delegate.tailMap(v).keySet()){
            allocateTree(key);
            result.putAll(delegate.get(key).tailMap(v));
        }
        return result;
    }

    @Override
    public K firstKey() {
        allocateTree(delegate.firstKey());
        return delegate.get(delegate.firstKey()).firstKey();
    }

    @Override
    public K lastKey() {
        allocateTree(delegate.lastKey());
        if(delegate.isEmpty())
            return null;
        if(delegate.get(delegate.lastKey()).isEmpty())
            return null;
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
        return entries.isEmpty();
    }

    @Override
    public V get(Object o) {
        if (delegate.isEmpty())
            return null;
        K last = delegate.lastKey();
        TreeMap<K,V> treeMap = allocateTree(last);
        if(treeMap.lastEntry()==null)
            return null;
        V ret = treeMap.lastEntry().getValue();
        unallocateTrees();
        return ret;
    }

    @Override
    public V put(K k, V v) {
        entries.add(k);
        if (!delegate.isEmpty()) {
            K last = delegate.lastKey();
            allocateTree(last);
            if (delegate.get(last).size()<threshhold) {
                return delegate.get(last).put(k, v);
            }
        }
        delegate.put(k,null);
        allocateTree(k);
        return delegate.get(k).put(k, v);
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        for(K k : map.keySet()){
            put(k, map.get(k));
        }
        unallocateTrees();
    }

    private TreeMap<K,V> allocateTree(K k){
        if(delegate.get(k)==null){
            TreeMap<K,V> treeMap = factory.getInstanceOf(TreeMap.class, k, true, null, false); // cause this is monotonically growing
            delegate.put(k, treeMap);
        }
        return delegate.get(k);
    }

    private void unallocateTrees(){
        for(K k : delegate.keySet()){
            if(delegate.get(k)!=null){
                try {
                    factory.disposeInstanceOf(TreeMap.class,k,true);
                } catch (IOException e) {
                    e.printStackTrace();  // TODO: Customise this generated block
                }
                delegate.put(k,null);
            }
        }
    }

    //
    // NOT YET IMPLEMENTED
    //

    @Override
    public V remove(Object o) {
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
