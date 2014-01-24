package org.infinispan.container.versioning;

import java.io.Serializable;
import java.util.Comparator;

/**
  *
 * @author Pierre Sutra
 * @since 6.0
 */
public class EntryVersionComparator implements Comparator<IncrementableEntryVersion>, Serializable{

    public EntryVersionComparator(){}

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
