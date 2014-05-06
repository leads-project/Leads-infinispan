package org.infinispan.atomic;

import org.infinispan.Cache;
import java.io.Externalizable;

/**
 *
 * @author Pierre Sutra
 * @since 7.0
 */

public abstract class AtomicObject implements Externalizable{
    protected transient Cache cache = null;
    protected transient Object key = null;
    protected transient String[] romethods = {};
}

