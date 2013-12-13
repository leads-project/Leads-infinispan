package org.infinispan.ensemble;

import org.infinispan.commons.api.BasicCacheContainer;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

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
    private transient BasicCacheContainer container;

    public Site(String name, BasicCacheContainer container) {
        this.name = name;
        this.container= container;
    }

    public String getName(){
        return name;
    }

    public BasicCacheContainer getContainer(){
        return container;
    }

    @Override
    public boolean equals(Object o){
        if(!(o instanceof Site)) return false;
        return ((Site)o).getName().equals(this.getName());
    }


    //
    // Serializable management
    //

    @SuppressWarnings("unchecked")
    public Object readResolve() {
        synchronized(this.getClass()){
            for(Site site: _sites.values()){
                if(site.equals(this))
                    return site;
            }
            _sites.put(name,this);
        }
        return this;
    }

}
