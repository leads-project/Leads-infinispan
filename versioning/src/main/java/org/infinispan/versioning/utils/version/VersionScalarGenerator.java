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

}
