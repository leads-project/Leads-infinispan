package org.infinispan.atomic;

/**
*
* @author Pierre Sutra
* @since 6.0
*/
class AtomicObjectCallPersist extends AtomicObjectCall{
    Object object;
    public AtomicObjectCallPersist(long id, Object o) {
        super(id);
        object = o;
    }

    @Override
    public String toString() {
        return super.toString()+"(PER)";
    }
}
