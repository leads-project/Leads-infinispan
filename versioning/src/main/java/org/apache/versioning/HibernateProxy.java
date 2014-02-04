package org.apache.versioning;

import org.hibernate.search.annotations.*;
import org.infinispan.container.versioning.IncrementableEntryVersion;

/**
 * // TODO: Document this
 *
 * @author Pierre Sutra
 * @since 6.0
 */

@Indexed
public class HibernateProxy<K,V> {

    @Field(index= Index.NO, analyze= Analyze.NO, store= Store.YES)
    @FieldBridge(impl = DummyFieldBridge.class)
    K k;

    @Field(index= Index.NO, analyze= Analyze.NO, store= Store.YES)
    @FieldBridge(impl = DummyFieldBridge.class)
    V v;

    @DocumentId
    @Field(index= Index.YES, analyze= Analyze.NO, store= Store.YES)
    @FieldBridge(impl = EntryVersionFieldBridge.class)
    IncrementableEntryVersion version;

    public HibernateProxy(K k, V v, IncrementableEntryVersion version){
        this.k = k;
        this.v = v;
        this.version = version;
    }

}
