package org.infinispan.container.versioning;

import java.io.Serializable;
import java.util.Comparator;
import java.util.TreeMap;

/**
 * // TODO: Document this
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public class EntryVersionTreeMap<IncrementableEntryVersion,V> extends TreeMap<IncrementableEntryVersion,V> implements Serializable{

    public EntryVersionTreeMap(){
        super((Comparator<? super IncrementableEntryVersion>) new EntryVersionComparator());
    }

}
