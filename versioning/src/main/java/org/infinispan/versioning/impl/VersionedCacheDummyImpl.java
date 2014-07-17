package org.infinispan.versioning.impl;

import org.infinispan.Cache;
import org.infinispan.commons.CacheException;
import org.infinispan.commons.marshall.jboss.GenericJBossMarshaller;
import org.infinispan.versioning.utils.version.Version;
import org.infinispan.versioning.utils.version.VersionGenerator;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;


/**
 *
 * @author Pierre Sutra
 * @since 7.0
 */
public class VersionedCacheDummyImpl<K,V> extends VersionedCacheAbstractImpl<K,V> {

    private static Log log = LogFactory.getLog(VersionedCacheDummyImpl.class);

    private GenericJBossMarshaller marshaller = new GenericJBossMarshaller();

    public VersionedCacheDummyImpl(Cache cache,
                                   VersionGenerator generator, String cacheName) {
        super(cache, generator, cacheName);
    }


    @Override
    public boolean isEmpty() {
        return  true;
    }

    @Override
    public Set<K> keySet() {
        return delegate.keySet();
    }

    @Override
    protected SortedMap<Version, V> versionMapGet(K key) {
        TreeMap<Version,V> map = retrieveTreeMap(key);
        if(map==null)
            return  new TreeMap<Version, V>();
        return map;
    }

    @Override
    protected void versionMapPut(K key, V value, Version version) {
        TreeMap<Version,V> treeMap = retrieveTreeMap(key);
        if(treeMap==null)
            treeMap = new TreeMap<Version, V>();
        treeMap.put(version, value);
        storeTreeMap(key, treeMap);
    }

    @Override
    public void putAll(K key, Map<Version,V> map){
        TreeMap<Version,V> treeMap = retrieveTreeMap(key);
        if(treeMap==null)
            treeMap = new TreeMap<Version, V>();
        treeMap.putAll(map);
        storeTreeMap(key, treeMap);
    }

    @Override
    public boolean containsKey(Object o) {
        return delegate.containsKey(o);
    }

    // PRIVATE METHODS

    private TreeMap<Version,V> retrieveTreeMap(K key){

        byte[] bb = (byte[]) delegate.get(key);

        if(bb==null)
            return null;

        try {
            return (TreeMap<Version,V>) marshaller.objectFromByteBuffer(bb);
        } catch (IOException | ClassNotFoundException e) {
            throw new CacheException(e);
        }
    }

    private void storeTreeMap(K k, TreeMap<Version, V> treeMap){
        try {
            byte[] bb = marshaller.objectToByteBuffer(treeMap);
            log.debug("Executing put with "+((float)bb.length/(float)1000)+" KB.");
            delegate.put(k,bb);
            log.debug("Done.");
        } catch (IOException | InterruptedException e) {
            throw new CacheException(e);
        }
    }

}
