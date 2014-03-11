package org.infinispan.versioning.impl;

import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.infinispan.Cache;
import org.infinispan.container.versioning.IncrementableEntryVersion;
import org.infinispan.container.versioning.VersionGenerator;
import org.infinispan.query.CacheQuery;
import org.infinispan.query.SearchManager;
import org.infinispan.versioning.utils.version.IncrementableEntryVersionComparator;
import org.infinispan.versioning.utils.hibernate.HibernateProxy;

import java.util.*;

/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public class VersionedCacheHibernateImpl<K,V> extends VersionedCacheAbstractImpl<K,V> {

    private SearchManager searchManager;

    public VersionedCacheHibernateImpl(Cache delegate, VersionGenerator generator, String name) {
        super(delegate, generator, name);
        // TODO check that the delegate is correct
        searchManager = org.infinispan.query.Search.getSearchManager(delegate);
    }

    protected SortedMap<IncrementableEntryVersion,V> versionMapGet(K key){
        QueryBuilder qb = searchManager.buildQueryBuilderForClass(HibernateProxy.class).get();
        Query q = qb.keyword().onField("k").matching(key).createQuery();
        CacheQuery cq = searchManager.getQuery(q, HibernateProxy.class);
        TreeMap<IncrementableEntryVersion,V> map = new TreeMap<IncrementableEntryVersion, V>(
                new IncrementableEntryVersionComparator());
        for(Object proxy : cq.list())
            map.put(((HibernateProxy<K,V>)proxy).version,((HibernateProxy<K,V>)proxy).v);
        return map;
    }

    @Override
    public V get(K key, IncrementableEntryVersion version) {
        QueryBuilder qb = searchManager.buildQueryBuilderForClass(HibernateProxy.class).get();
        Query q = qb.bool()
                .must(qb.keyword().onField("k").matching(key).createQuery())
                .must(qb.keyword().onField("version").matching(version).createQuery())
                .createQuery();
        CacheQuery cq = searchManager.getQuery(q, HibernateProxy.class);
        List list = cq.list();
        if(list.isEmpty())
            return null;
        assert list.size()==1 : list.toString();
        return ((HibernateProxy<K,V>)list.get(0)).v;
    }

//    @Override
//    public Collection<V> get(K key, IncrementableEntryVersion first, IncrementableEntryVersion last) {
//        Set<V> result = new HashSet<V>();
//        QueryBuilder qb = searchManager.buildQueryBuilderForClass(HibernateProxy.class).get();
//        Query q = qb.bool()
//                .must(qb.keyword().onField("k").matching(key).createQuery())
//                .must(qb.range().onField("version").above(first).createQuery())
//                .must(qb.range().onField("version").below(last).createQuery())
//                .createQuery();
//        CacheQuery cq = searchManager.getQuery(q, HibernateProxy.class);
//        for(Object proxy : cq.list())
//            result.add(((HibernateProxy<K,V>)proxy).v);
//        return  result;
//    }


    @Override
    protected void versionMapPut(K key, V value, IncrementableEntryVersion version) {
        HibernateProxy<K,V> proxy = new HibernateProxy<K, V>(key,value,version);
        ((Cache<String,HibernateProxy<K,V>>)delegate).put(proxy.getId(), proxy);
    }

    @Override
    public boolean isEmpty() {
        QueryBuilder qb = searchManager.buildQueryBuilderForClass(HibernateProxy.class).get();
        Query q = qb.all().createQuery();
        CacheQuery cq = searchManager.getQuery(q, HibernateProxy.class);
        return cq.list().isEmpty();
    }

    @Override
    public boolean containsKey(Object o) {
        QueryBuilder qb = searchManager.buildQueryBuilderForClass(HibernateProxy.class).get();
        Query q = qb.keyword().onField("k").matching(o).createQuery();
        CacheQuery cq = searchManager.getQuery(q, HibernateProxy.class);
        return cq.list().isEmpty();
    }

    @Override
    public boolean containsValue(Object o) {
        QueryBuilder qb = searchManager.buildQueryBuilderForClass(HibernateProxy.class).get();
        Query q = qb.keyword().onField("v").matching(o).createQuery();
        CacheQuery cq = searchManager.getQuery(q, HibernateProxy.class);
        return cq.list().isEmpty();
    }

    @Override
    public Set<K> keySet() {
        HashSet<K> result = new HashSet<K>();
        QueryBuilder qb = searchManager.buildQueryBuilderForClass(HibernateProxy.class).get();
        Query q = qb.all().createQuery();
        CacheQuery cq = searchManager.getQuery(q, HibernateProxy.class);
        for(Object proxy : cq.list())
            result.add(((HibernateProxy<K,V>)proxy).k);
        return result;
    }
}
