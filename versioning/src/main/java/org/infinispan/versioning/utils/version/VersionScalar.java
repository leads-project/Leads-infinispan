package org.infinispan.versioning.utils.version;

import java.io.Serializable;

/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public class VersionScalar extends Version implements Serializable{

    private Integer scalar;

    public VersionScalar(){
        scalar=0;
    }

    public VersionScalar(VersionScalar v){
        scalar=v.scalar;
    }

    @Override
    public int compareTo(Version version) {
        if(!(version instanceof VersionScalar))
            throw new IllegalArgumentException();
        return scalar.compareTo(((VersionScalar)version).scalar);
    }

    @Override
    public void increment() {
        scalar++;
    }

    public int value(){
        return scalar;
    }

    @Override
    public int hashCode(){
        return scalar;
    }

}
