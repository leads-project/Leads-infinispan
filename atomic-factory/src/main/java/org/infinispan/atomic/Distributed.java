package org.infinispan.atomic;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Pierre Sutra
 */

@Retention(value = RetentionPolicy.RUNTIME)
public @interface Distributed {
}
