package org.infinispan.versioning.impl;

import org.infinispan.Cache;
import org.infinispan.atomic.AtomicObjectFactory;
import org.infinispan.versioning.utils.version.Version;
import org.infinispan.versioning.utils.version.VersionGenerator;
import org.jboss.logging.Logger;

import java.util.*;

/**
 *
 * @author Pierre Sutra
 * @since 7.0
 */
public class VersionedCacheashMapImpl<K,V> extends VersionedCacheSplitAbstractImpl<K,V> {

    AtomicObjectFactory factory;
    Logger logger;
    Class mapClass;

    public VersionedCacheashMapImpl(Cache delegate, VersionGenerator generator, String name) {
        super(delegate,generator,name);
        factory = new AtomicObjectFactory((Cache<Object, Object>) delegate);
        this.logger  = Logger.getLogger(this.getClass());

    }
    @Override
    protected SortedMap<Version, String> versionMapGet(K key) {
        TreeMap<Version,String> treeMap = new TreeMap(factory.getInstanceOf(HashMap.class,key,true,null,false));
        factory.disposeInstanceOf(HashMap.class, key, true);
        return treeMap;
    }

    @Override
    protected void versionMapPut(K key, String value, Version version) {
        HashMap<Version,String> HashMap = factory.getInstanceOf(HashMap.class, key, true, null, false);
        HashMap.put(version, value);
        factory.disposeInstanceOf(HashMap.class,key,true);
    }

    @Override
    protected void versionMapPutAll(K key, Map<Version, String> map) {
        HashMap<Version,String> HashMap  = factory.getInstanceOf(HashMap.class, key, true, null, false);
        HashMap.putAll(map);
        factory.disposeInstanceOf(HashMap.class,key,true);
    }

}

