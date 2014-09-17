package org.infinispan.ensemble.indexing;

import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author Pierre Sutra
 * @since 7.0
 */
public interface IndexBuilder {

    public <K,V extends Indexable> ConcurrentMap<K,V> getIndex(Class<V> clazz);

}
