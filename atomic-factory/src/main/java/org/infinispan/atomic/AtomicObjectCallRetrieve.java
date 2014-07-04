package org.infinispan.atomic;

/**
*
* @author Pierre Sutra
* @since 6.0
*/
class AtomicObjectCallRetrieve extends AtomicObjectCall{
    public AtomicObjectCallRetrieve(long id) {
        super(id);
    }

    @Override
    public String toString() {
        return super.toString()+"(RET)";
    }


}
