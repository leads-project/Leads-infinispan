package org.apache.versioning;

import org.infinispan.container.versioning.IncrementableEntryVersion;
import org.infinispan.container.versioning.InequalVersionComparisonResult;

import java.io.Serializable;
import java.util.Comparator;

/**
  *
 * @author Pierre Sutra
 * @since 6.0
 */
public class IncrementableEntryVersionComparator implements Comparator<IncrementableEntryVersion>, Serializable{

    public IncrementableEntryVersionComparator(){}

    @Override
    public int compare(IncrementableEntryVersion o, IncrementableEntryVersion o2) {
        if(o.compareTo(o2).equals(InequalVersionComparisonResult.AFTER))
            return 1;
        if(o.compareTo(o2).equals(InequalVersionComparisonResult.BEFORE))
            return -1;
        if(o.compareTo(o2).equals(InequalVersionComparisonResult.EQUAL))
            return 0;
        throw new IllegalArgumentException("concurrent versions");
    }

}
