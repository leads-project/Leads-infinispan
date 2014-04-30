package org.infinispan.atomic;

import java.io.Serializable;

/**
*
*
* @author Pierre Sutra
* @since 6.0
*/
abstract class AtomicObjectCall implements Serializable {
    long callID;
    public AtomicObjectCall(long id){
        callID = id;
    }

    @Override
    public String toString(){
        return Long.toString(callID);
    }
}
