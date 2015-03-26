package org.infinispan.atomic.container;

import java.util.UUID;

/**
* @author Pierre Sutra
* @since 7.0
*/
class CallPersist extends Call {

    Object object;

    public CallPersist(UUID id, Object o) {
        super(id);
        object = o;
    }

    @Override
    public String toString() {
        return super.toString()+"(PER)";
    }
}
