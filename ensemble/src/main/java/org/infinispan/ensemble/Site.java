package org.infinispan.ensemble;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.CacheException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */

public class Site implements Serializable{

    private String name;
    private transient RemoteCacheManager container;

    public Site(String name, RemoteCacheManager container) {
        this.name = name;
        this.container= container;
    }

    public String getName(){
        return name;
    }

    public RemoteCacheManager getManager(){
        return container;
    }

    @Override
    public boolean equals(Object o){
        if( !(o instanceof Site) ) return false;
        return ((Site)o).getName().equals(this.getName());
    }

}
