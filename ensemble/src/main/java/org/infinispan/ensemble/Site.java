package org.infinispan.ensemble;

import org.infinispan.client.hotrod.RemoteCacheManager;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */

public class Site implements Serializable{

    public transient static Map<String,Site> _sites;
    public transient static Site localSite;
    static{
        _sites = new TreeMap<String, Site>();
    }

    private String name;
    private transient boolean isLocal;
    private transient RemoteCacheManager container;

    public Site(String name, RemoteCacheManager container, boolean isLocal) {
        this.name = name;
        this.isLocal = isLocal;
        this.container= container;
        synchronized(this.getClass()){
            if(_sites.containsKey(name)){
                System.out.println("Already existing site: "+name);
            }else{
                System.out.println("Adding site: "+name);
                if(isLocal && localSite==null)
                    localSite = this;
                _sites.put(name,this);
            }
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
        return "@"+name;
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

    public static Site localSite() {
        return localSite;
    }
}
