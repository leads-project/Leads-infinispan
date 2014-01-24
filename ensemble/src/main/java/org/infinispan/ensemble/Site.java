package org.infinispan.ensemble;

import org.infinispan.client.hotrod.RemoteCacheManager;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.infinispan.commons.CacheException;

/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */

public class Site implements Serializable{

    private static Map<String,Site> _sites;
    static{
        _sites = new HashMap<String, Site>();
    }

    private String name;
    private transient RemoteCacheManager container;

    public Site(String name, RemoteCacheManager container) {
        this.name = name;
        this.container= container;
        synchronized(this.getClass()){
            if(_sites.containsValue(this))
                throw new CacheException("Already existing site: "+name);
            _sites.put(name,this);
        }
    }

    public String getName(){
        return name;
    }

    public RemoteCacheManager getContainer(){
        return container;
    }

    @Override
    public boolean equals(Object o){
        if( !(o instanceof Site) ) return false;
        return ((Site)o).getName().equals(this.getName());
    }


    //
    // Serializability management
    //

    @SuppressWarnings("unchecked")
    public Object readResolve() {
        synchronized(this.getClass()){
            for(Site site: _sites.values()){
                if(site.equals(this))
                    return site;
            }
            throw new CacheException("No definition for site: "+this.name);
        }
    }

}
