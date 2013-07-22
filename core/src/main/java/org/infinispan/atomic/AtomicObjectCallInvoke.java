package org.infinispan.atomic;

/**
* // TODO: Document this
*
* @author otrack
* @since 4.0
*/
class AtomicObjectCallInvoke extends AtomicObjectCall{
    String method;
    Object[] arguments;
    public AtomicObjectCallInvoke(int id, String m, Object[] args) {
        super(id);
        method = m;
        arguments = args;
    }
    @Override
    public String toString(){
        return method+ "(" + arguments.toString() + ")";
    }
}
