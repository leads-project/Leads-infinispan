package org.infinispan.atomic.collections;

import org.infinispan.atomic.AtomicObject;
import org.infinispan.atomic.AtomicObjectFactory;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;



/**
 *
 * A sorted map abstraction implemented via an ordered forest of trees.
 * The ordered forest of trees is stored in variable <i>forest</i>.
 * Each trees is a shared object implemented with the atomic object factory.
 * It contains at most <i>threshold</i> objects.
 *
 * @author Pierre Sutra
 * @since 7.0
 *
 */
public class ShardedTreeMap<K,V> extends AtomicObject implements SortedMap<K, V>
{

    private static Log log = LogFactory.getLog(ShardedTreeMap.class);
    private final static int DEFAULT_THRESHOLD = 1000 ;

    private SortedMap<K,TreeMap<K,V>> forest; // the ordered forest
    private int threshold; // how many entries are stored before creating a new tree in the forest.

    public ShardedTreeMap(){
        forest = new TreeMap<K, TreeMap<K,V>>();
        threshold = DEFAULT_THRESHOLD;
    }

    public ShardedTreeMap(Integer threshhold){
        assert threshhold>1;
        forest = new TreeMap<K, TreeMap<K,V>>();
        this.threshold = threshhold;
    }

    @Override
    public SortedMap<K, V> subMap(K v1, K v2) {
        SortedMap<K,V> result = new TreeMap<K, V>();
        for(K key : forest.keySet()){
            allocateTree(key);
            result.putAll(forest.get(key).subMap(v1, v2));
        }
        unallocateTrees();
        return result;
    }

    @Override
    public SortedMap<K, V> headMap(K toKey) {
        SortedMap<K,V> result = new TreeMap<K, V>();
        for(K key : forest.keySet()){
            allocateTree(key);
            result.putAll(forest.get(key).headMap(toKey));
        }
        unallocateTrees();
        return result;
    }

    @Override
    public SortedMap<K, V> tailMap(K fromKey) {
        SortedMap<K,V> result = new TreeMap<K, V>();
        for(K key : forest.keySet()){
            allocateTree(key);
            result.putAll(forest.get(key).tailMap(fromKey));
        }
        unallocateTrees();
        return result;
    }

    @Override
    public K firstKey() {
        if(forest.isEmpty())
            return null;
        allocateTree(forest.firstKey());
        assert !forest.get(forest.firstKey()).isEmpty() : forest.toString();
        K ret = forest.get(forest.firstKey()).firstKey();
        unallocateTrees();
        return ret;
    }

    @Override
    public K lastKey() {
        if(forest.isEmpty())
            return null;
        allocateTree(forest.lastKey());
        assert !forest.get(forest.lastKey()).isEmpty(): forest.toString();
        K ret = forest.get(forest.lastKey()).lastKey();
        unallocateTrees();
        return ret;
    }

    @Override
    public int size() {
        int ret = 0;
        for(K v: forest.keySet()){
            allocateTree(v);
            ret+= forest.get(v).size();
        }
        unallocateTrees();
        return ret;
    }

    @Override
    public boolean isEmpty() {
        return forest.isEmpty();
    }

    @Override
    public V get(Object o) {
        if (forest.isEmpty())
            return null;
        K last = forest.lastKey();
        TreeMap<K,V> treeMap = allocateTree(last);
        assert !forest.get(last).isEmpty();
        V ret = treeMap.lastEntry().getValue();
        unallocateTrees();
        return ret;
    }

    @Override
    public V put(K k, V v) {
        log.debug("adding "+k+"="+v);
        V ret = null;
        if (forest.isEmpty()){
            forest.put(k,null);
            allocateTree(k);
            forest.get(k).put(k, v);
        }else if (forest.containsKey(k)) {
            ret = forest.get(k).put(k,v);
        }else{
            K last = forest.headMap(k).lastKey();
            allocateTree(last);
            ret = forest.get(last).put(k,v);
            if (forest.get(last).size() > threshold){ // FIXME assumption on monotonically growing insertions
                Entry<K,V> entry = forest.get(last).lastEntry();
                forest.get(last).remove(entry.getKey());
                forest.put(entry.getKey(),null);
                allocateTree(entry.getKey());
                forest.get(entry.getKey()).put(entry.getKey(), v);
            }
        }
        unallocateTrees();
        return ret;
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return forest.hashCode();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        for(K k : map.keySet()){
            put(k, map.get(k));
        }
    }


    @Override
    public String toString(){
        TreeMap<K,V> all = new TreeMap<K, V>();
        for(K key : forest.keySet()){
            allocateTree(key);
            all.putAll(forest.get(key));
        }
        unallocateTrees();
        return all.toString();
    }

    //
    // MARSHALLING
    //

    @Override
    public void writeExternal(ObjectOutput objectOutput) throws IOException {
        objectOutput.writeObject(new ArrayList<K>(forest.keySet()));
    }

    @Override
    public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
        forest = new TreeMap<K, TreeMap<K,V>>();
        for( K k : (List<K>)objectInput.readObject()){
            forest.put(k,null);
        }
    }


    //
    // HELPERS
    //

    private TreeMap<K,V> allocateTree(K k){
        log.debug("Allocating "+k);
        if(forest.get(k)==null){
            assert this.key!=null;
            TreeMap treeMap = AtomicObjectFactory.forCache(this.cache).getInstanceOf(
                    TreeMap.class, this.key.toString()+":"+k.toString(), true, null, false);
            forest.put(k, treeMap);
            log.debug("... done ");
        }
        return forest.get(k);
    }

    private void unallocateTrees(){
        List<K> toUnallocate = new ArrayList<K>();
        for(K k : forest.keySet()){
            if(forest.get(k)!=null){
                toUnallocate.add(k);
            }
        }
        for(K k : toUnallocate){
            log.debug("Unallocate "+k);
            AtomicObjectFactory.forCache(this.cache).disposeInstanceOf(
                    TreeMap.class, this.key.toString()+":"+k.toString(), true);
            forest.put(k,null);
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
