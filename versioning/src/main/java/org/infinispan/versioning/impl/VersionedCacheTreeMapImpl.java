package org.infinispan.versioning.impl;

import org.infinispan.Cache;
import org.infinispan.atomic.AtomicObjectFactory;
import org.infinispan.versioning.utils.version.Version;
import org.infinispan.versioning.utils.version.VersionGenerator;
import org.jboss.logging.Logger;

import java.util.*;

/**
 * // TODO: Document this
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public class VersionedCacheTreeMapImpl<K,V> extends VersionedCacheSplitAbstractImpl<K,V> {

    AtomicObjectFactory factory;
    Logger logger;
    public VersionedCacheTreeMapImpl(Cache delegate, VersionGenerator generator, String name) {
        super(delegate,generator,name);
        factory = new AtomicObjectFactory((Cache<Object, Object>) delegate);
        this.logger  = Logger.getLogger(this.getClass());

    }

    @Override
    protected SortedMap<Version, String> versionMapGetStart(K key) {
        TreeMap<Version,String> treeMap = factory.getInstanceOf(TreeMap.class,key,true,null,false);
        return treeMap;
    }

    @Override
    protected void versionMapGetEnd(K key) {
        factory.disposeInstanceOf(TreeMap.class, key, true);
    }

    @Override
    protected void versionMapPut(K key, String value, Version version) {
        TreeMap<Version,String> treeMap = factory.getInstanceOf(TreeMap.class, key, true, null, false);
        treeMap.put(version, value);
        factory.disposeInstanceOf(TreeMap.class,key,true);
    }

    @Override
    protected void versionMapPutAll(K key, Map<Version, String> map) {
        TreeMap<Version,String> treeMap  = factory.getInstanceOf(TreeMap.class, key, true, null, false);
        treeMap.putAll(map);
        factory.disposeInstanceOf(TreeMap.class,key,true);
    }

}
