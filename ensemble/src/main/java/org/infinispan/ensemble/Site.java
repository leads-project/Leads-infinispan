package org.infinispan.ensemble;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.ensemble.cache.EnsembleCache;
import org.infinispan.ensemble.cache.SiteEnsembleCache;
import org.infinispan.ensemble.indexing.Indexable;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * A site is a geographical location where an ISPN instance is deployed.
 * This deployment is accessed via the container field of the site.
 * A single site can be marked local.
 *
 * @author Pierre Sutra
 * @since 6.0
 */

public class Site extends Indexable {


    //
    // CLASS FIELDS
    //
    private static final Log log = LogFactory.getLog(Site.class);
    private transient static Site localSite;

    //
    // OBJECT FIELDS
    //

    private String name;
    private transient boolean isLocal;
    private transient RemoteCacheManager container;

    //
    // PUBLIC METHODS
    //

    public static Site localSite() {
        return localSite;
    }

    /**
     *
     * By convention, the first name is the local site.
     * @param sites
     * @return
     */
    public static Collection<Site> fromNames(Collection<String> sites){
        List<Site> list = new ArrayList<Site>();
        boolean local=true;
        for(String s: sites){
            Site site = new Site(s, new RemoteCacheManager(s),local);
            if(local) local=false;
            list.add(site);
        }
        return list;
    }

    public Site(String name, RemoteCacheManager container, boolean isLocal) {
        this.name = name;
        this.isLocal = isLocal;
        this.container= container;
        synchronized(this.getClass()){
            if(isLocal && localSite==null)
                localSite = this;
        }
    }

    public String getName(){
        return name;
    }

    public RemoteCacheManager getManager(){
        return container;
    }

    public <K,V> EnsembleCache<K,V> getCache(String name){
        return new SiteEnsembleCache<K, V>(name,container.getCache(name));
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


}
