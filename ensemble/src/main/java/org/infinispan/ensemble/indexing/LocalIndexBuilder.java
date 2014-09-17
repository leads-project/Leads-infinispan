package org.infinispan.ensemble.indexing;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author Pierre Sutra
 * @since 7.0
 */
public class LocalIndexBuilder implements IndexBuilder {

    public LocalIndexBuilder(){}

    @Override
    public <K, V extends Indexable> ConcurrentMap<K, V> getIndex(Class<V> clazz) {
        return new ConcurrentHashMap<K, V>();
    }

}
