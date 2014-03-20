package org.infinispan.atomic;

import org.infinispan.Cache;

/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public abstract class AtomicShardedObject {

    protected AtomicObjectFactory factory;

    @Deprecated
    public AtomicShardedObject(){}

    public AtomicShardedObject(Cache c){
        factory = AtomicObjectFactory.forCache(c);
    }

}
