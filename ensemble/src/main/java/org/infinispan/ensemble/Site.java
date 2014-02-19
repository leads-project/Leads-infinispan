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

    public transient static Map<String,Site> _sites;
    static{
        _sites = new HashMap<String, Site>();
    }

    private String name;
    private transient RemoteCacheManager container;

    public Site(String name, RemoteCacheManager container) {
        this.name = name;
        this.container= container;
        synchronized(this.getClass()){
            if(_sites.containsKey(name)){
                if(_sites.get(name).container != null){
                    throw new CacheException("Already existing site: "+name);
                }
            }
            _sites.put(name,this);
        }
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


    @Override
    public String toString(){
        return name;
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
            _sites.put(this.name,this);
            return this;
        }
    }

}
