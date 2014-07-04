package org.infinispan.versioning.utils.version;

/**
 *
 * @author Pierre Sutra
 * @since 7.0
 */
public abstract class VersionGenerator {

    public abstract Version generateNew();
    public abstract Version increment(Version v);
}
