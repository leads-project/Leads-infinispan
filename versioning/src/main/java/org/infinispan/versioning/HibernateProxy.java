package org.infinispan.versioning;

import org.hibernate.search.annotations.*;
import org.infinispan.container.versioning.IncrementableEntryVersion;
import org.infinispan.container.versioning.NumericVersion;

import java.io.Serializable;

/**
 * // TODO: Document this
 *
 * @author Pierre Sutra
 * @since 6.0
 */

@Indexed
public class HibernateProxy<K,V> implements Serializable{

    @DocumentId
    @Field(index= Index.YES, analyze= Analyze.NO, store= Store.NO)
    @FieldBridge(impl = DummyFieldBridge.class)
    K k;

//    @Field(index= Index.NO, analyze= Analyze.NO, store= Store.YES)
//    @FieldBridge(impl = DummyFieldBridge.class)
    V v;

    @Field(index= Index.YES, analyze= Analyze.NO, store= Store.NO)
    @FieldBridge(impl = EntryVersionFieldBridge.class)
    IncrementableEntryVersion version;

    public HibernateProxy(K k, V v, IncrementableEntryVersion version){
        this.k = k;
        this.v = v;
        this.version = version;
    }

    public String getId(){
        return k.toString()+Long.toString( ((NumericVersion)version).getVersion() );
    }

    public String toString(){
        return "HibernateProxy{"+this.k.toString()+","+this.version.toString()+"}";
    }

}
