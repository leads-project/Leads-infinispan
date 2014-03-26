package org.infinispan.versioning.utils.version;

/**
 * // TODO: Document this
 *
 * @author otrack
 * @since 4.0
 */
public abstract class VersionGenerator {

    public abstract Version generateNew();
    public abstract Version increment(Version v);
}
