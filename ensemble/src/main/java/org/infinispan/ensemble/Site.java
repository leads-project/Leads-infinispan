package org.infinispan.ensemble;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * A site is a geographical location where an ISPN instance is deployed.
 * This deployment is accessed via the container field of the site.
 * A single site can be marked local.
 *
 * @author Pierre Sutra
 * @since 6.0
 */

public class Site implements Serializable{


    //
    // CLASS FIELDS
    //

    public transient static Map<String,Site> _sites;
    public transient static Site localSite;
    static{
        _sites = new TreeMap<String, Site>();
    }
    private static final Log log = LogFactory.getLog(Site.class);

    //
    // OBJECT FIELDS
    //

    private String name;
    private transient boolean isLocal;
    private transient RemoteCacheManager container;

    //
    // PUBLIC METHODS
    //

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

    public static Site localSite() {
        return localSite;
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
