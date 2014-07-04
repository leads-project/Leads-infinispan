package org.infinispan.atomic;

/**
* *
* @author Pierre Sutra
* @since 6.0
*/
class AtomicObjectCallInvoke extends AtomicObjectCall{
    String method;
    Object[] arguments;

    public AtomicObjectCallInvoke(long id, String m, Object[] args) {
        super(id);
        method = m;
        arguments = args;
    }

    @Override
    public String toString(){
        String args = " ";
        for(Object a : arguments){
            args+=a.toString()+" ";
        }
        return super.toString()+"  - INV - "+method+ " ("+args+")";
    }
}
