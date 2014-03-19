package org.infinispan.versioning.utils.hibernate;

import org.hibernate.search.annotations.*;
import org.infinispan.versioning.utils.version.Version;

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
    public K k;

//    @Field(index= Index.NO, analyze= Analyze.NO, store= Store.YES)
//    @FieldBridge(impl = DummyFieldBridge.class)
    public V v;

    @Field(index= Index.YES, analyze= Analyze.NO, store= Store.NO)
    @FieldBridge(impl = EntryVersionFieldBridge.class)
    public Version version;

    public HibernateProxy(K k, V v, Version version){
        this.k = k;
        this.v = v;
        this.version = version;
    }

    public String getId(){
        return k.toString()+version.hashCode();
    }

    public String toString(){
        return "HibernateProxy{"+this.k.toString()+","+this.version.toString()+"}";
    }

}
