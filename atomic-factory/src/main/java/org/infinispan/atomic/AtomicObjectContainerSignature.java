package org.infinispan.atomic;

/**
*
* @author Pierre Sutra
* @since 7.0
 *
*/
public class AtomicObjectContainerSignature {

    private Class clazz;
    private Object key;
    private int hash;

    public AtomicObjectContainerSignature(Class c, Object k){
        clazz = c;
        key = k;
        hash = clazz.hashCode() + key.hashCode();
    }

    @Override
    public int hashCode(){
        return hash;
    }

    @Override
    public boolean equals(Object o){
        if (!(o instanceof AtomicObjectContainerSignature))
            return false;
        return ((AtomicObjectContainerSignature)o).hash == this.hash;
    }

    @Override
    public String toString(){
        return key.toString()+"["+clazz.getSimpleName()+"]";
    }

}
