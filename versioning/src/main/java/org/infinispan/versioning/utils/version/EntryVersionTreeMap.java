package org.infinispan.versioning.utils.version;

import java.io.Serializable;
import java.util.Comparator;
import java.util.TreeMap;

/**
 *
 * This class helps implementing the VersionedCacheAtomicTreeMaImpl,
 * that is, when all the versions of a key are stored inside a TreeMap.
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public class EntryVersionTreeMap<IncrementableEntryVersion,V> extends TreeMap<IncrementableEntryVersion,V> implements Serializable{

    public EntryVersionTreeMap(){
        super((Comparator<? super IncrementableEntryVersion>) new IncrementableEntryVersionComparator());
    }

}
