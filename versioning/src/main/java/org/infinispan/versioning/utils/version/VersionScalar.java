package org.infinispan.versioning.utils.version;

import java.io.Serializable;

/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public class VersionScalar extends Version implements Serializable{

    private long scalar;

    public VersionScalar(){
        scalar = 0L;
    }

    public VersionScalar(long value){
        scalar = value;
    }

    public VersionScalar(VersionScalar v){
        scalar = v.scalar;
    }

    @Override
    public int compareTo(Version version) {
        if(!(version instanceof VersionScalar))
            throw new IllegalArgumentException();
        if (scalar > ((VersionScalar)version).scalar)
            return 1;
        if (scalar < ((VersionScalar)version).scalar)
            return -1;
        return 0;
    }

    @Override
    public void increment() {
        scalar ++;
    }

    public long version(){
        return scalar;
    }

    @Override
    public int hashCode(){
        return (int) scalar;
    }

    @Override
    public String toString() {
        return Long.toString(scalar);
    }
}
