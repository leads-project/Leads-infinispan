package org.infinispan.atomic.container;

import java.util.UUID;

/**
* @author Pierre Sutra
* @since 7.0
*/
class CallRetrieve extends Call {
   
    public CallRetrieve(UUID id) {
        super(id);
    }

    @Override
    public String toString() {
        return super.toString()+"(RET)";
    }


}
