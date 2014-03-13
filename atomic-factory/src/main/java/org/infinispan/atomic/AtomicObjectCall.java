package org.infinispan.atomic;

import java.io.Serializable;

/**
*
*
* @author Pierre Sutra
* @since 6.0
*/
abstract class AtomicObjectCall implements Serializable {
    int callID;
    public AtomicObjectCall(int id){
        callID = id;
    }
}
