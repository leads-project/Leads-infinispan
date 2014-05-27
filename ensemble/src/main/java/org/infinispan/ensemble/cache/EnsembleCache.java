package org.infinispan.ensemble.cache;

import org.infinispan.ensemble.indexing.Indexable;
import org.infinispan.ensemble.indexing.Primary;
import org.infinispan.ensemble.indexing.Stored;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;


/**
 *
 * An EnsembleCache offers a ConcurrentMap API over a list of EnsembleCaches.
 * Such an abstraction is of interest in various cases, e.g., when aggregating multiple Infinispan deployments.
 *
 * @author Pierre Sutra
 * @since 7.0
 */
public abstract class EnsembleCache<K,V> extends Indexable implements ConcurrentMap<K,V> {

    protected static final Log log = LogFactory.getLog(EnsembleCache.class);

    @Primary
    @Stored
    protected String name;

    @Stored
    protected List<? extends EnsembleCache<K,V>> caches;

    public EnsembleCache(String name, List<? extends EnsembleCache<K,V>> caches){
        this.name = name;
        this.caches= caches;
    }

    //
    // PUBLIC
    //

    public String getName() {
        return name;
    }

    //
    // NYI
    //


    // READ

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }


    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsKey(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsValue(Object o) {
        throw new UnsupportedOperationException();
    }

    // WRITE

    @Override
    public V putIfAbsent(K k, V v) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o, Object o2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean replace(K k, V v, V v2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V replace(K k, V v) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

}
