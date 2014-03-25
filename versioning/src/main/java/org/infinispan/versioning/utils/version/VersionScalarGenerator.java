package org.infinispan.versioning.utils.version;

/**
 * // TODO: Document this
 *
 * @author otrack
 * @since 4.0
 */

public class VersionScalarGenerator extends VersionGenerator {

    private VersionScalar current;

    public VersionScalarGenerator(){
        current = new VersionScalar();
    }

    @Override
    public synchronized Version generateNew() {
        current.increment();
        return new VersionScalar(current);
    }

    @Override
    public Version increment(Version v) {
        VersionScalar ret = new VersionScalar((VersionScalar)v);
        ret.increment();
        return ret;
    }

    @Override
    public synchronized Version generateFrom(Object objectValue) {
        VersionScalar ver;
        if (objectValue instanceof Long)
            ver = new VersionScalar(((Long) objectValue).longValue());
        else if (objectValue instanceof Integer)
            ver = new VersionScalar((long)((Integer) objectValue).intValue());
        else
            throw new IllegalArgumentException("Cannot generate ScalarVersion from object type " + objectValue.getClass().toString());

        if (ver.compareTo((Version)current) < 0 )
            throw new IllegalArgumentException("Cannot generate ScalarVersion in the past (current=" + current + " from=" + ver + ")");

        current = ver;
        return ver;
    }

}
