package org.infinispan.ensemble.zk;

import net.killa.kept.KeptConcurrentMap;
import org.infinispan.commons.util.Base64;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */

public class ZkConcurrentMap<K,V> implements ConcurrentMap<K,V> {

    private KeptConcurrentMap keptConcurrentMap;

    public ZkConcurrentMap(KeptConcurrentMap m){
        keptConcurrentMap = m;
    }


    @Override
    public V putIfAbsent(K k, V v) {
        Object o = keptConcurrentMap .put(marshal(k),marshal(v));
        System.out.println(o.getClass().toString());
        return null;
    }

    @Override
    public boolean remove(Object o, Object o2) {
        return keptConcurrentMap.remove(marshal(o), marshal(o2));
    }

    @Override
    public boolean replace(K k, V v, V v2) {
        return keptConcurrentMap.replace(marshal(k), marshal(v), marshal(v2));
    }

    @Override
    public V replace(K k, V v) {
        return (V) unmarshal(keptConcurrentMap.replace(marshal(k),marshal(v)));
    }

    @Override
    public int size() {
        return keptConcurrentMap.size();
    }

    @Override
    public boolean isEmpty() {
        return keptConcurrentMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object o) {
        return keptConcurrentMap.containsKey(marshal(o));
    }

    @Override
    public boolean containsValue(Object o) {
        return keptConcurrentMap.containsValue(marshal(o));
    }

    @Override
    public V get(Object o) {
        return (V) unmarshal(keptConcurrentMap.get(marshal(o)));
    }

    @Override
    public V put(K k, V v) {
        return (V) unmarshal(keptConcurrentMap.put(marshal(k),marshal(v)));
    }

    @Override
    public V remove(Object o) {
        return (V) unmarshal(keptConcurrentMap.remove(marshal(o)));
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        throw new RuntimeException("NYI");
    }

    @Override
    public void clear() {
        keptConcurrentMap.clear();
    }

    @Override
    public Set<K> keySet() {
        throw new RuntimeException("NYI");
    }

    @Override
    public Collection<V> values() {
        throw new RuntimeException("NYI");
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new RuntimeException("NYI");
    }

    public static <T> String marshal(T object)
    {
        return Base64.encodeObject((Serializable) object);
    }

    public static <T> Object unmarshal(String objectString)
    {
        if(objectString==null)
            return null;
        return Base64.decodeToObject(objectString);
    }

}
