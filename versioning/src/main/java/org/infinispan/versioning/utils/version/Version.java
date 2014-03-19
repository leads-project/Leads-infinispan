package org.infinispan.versioning.utils.version;

/**
 *
 * @author otrack
 * @since 4.0
 */
public abstract class Version implements Comparable<Version>, Cloneable {
    public abstract void increment();
}
