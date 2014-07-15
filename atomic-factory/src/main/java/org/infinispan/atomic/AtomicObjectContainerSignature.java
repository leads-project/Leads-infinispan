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

    public AtomicObjectContainerSignature(Class c, Object k){
        clazz = c;
        key = k;
    }

    @Override
    public int hashCode(){
        return clazz.hashCode() + key.hashCode();
    }

    @Override
    public boolean equals(Object o){
        if (!(o instanceof AtomicObjectContainerSignature))
            return false;
        return ((AtomicObjectContainerSignature)o).clazz.equals(this.clazz)
                && ((AtomicObjectContainerSignature)o).key.equals(this.key);
    }

    @Override
    public String toString(){
        return key.toString()+"["+clazz.getSimpleName()+"]";
    }

}
