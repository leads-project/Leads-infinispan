package org.infinispan.ensemble;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.ensemble.cache.SiteEnsembleCache;
import org.infinispan.ensemble.indexing.Indexable;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * A site is a geographical location where an ISPN instance is deployed.
 * This deployment is accessed via the container field of the site.
 * A single site can be marked local.
 *
 * @author Pierre Sutra, Marcelo Pasin
 * @since 6.0
 */

public class Site extends Indexable {


    //
    // CLASS FIELDS
    //
    private transient static final Log log = LogFactory.getLog(Site.class);
    private transient static Site localSite;

    //
    // OBJECT FIELDS
    //

    private String name;
    private transient boolean isLocal;
    private transient RemoteCacheManager container;

    //
    // CLASS METHODS
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
    public static Collection<Site> valuesOf(Collection<String> sites, Marshaller marshaller){

        List<Site> list = new ArrayList<Site>();

        boolean isLocal=true;

        for(String site: sites){
            list.add(
                    valueOf(site, marshaller, isLocal));
            if (isLocal) isLocal = false;
        }

        return list;
    }

    public static Site valueOf(String servers, Marshaller marshaller, boolean isLocal){

        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

        if (marshaller != null)
            configurationBuilder.marshaller(marshaller);

        for(String server : servers.split(";")) {
            String host;
            int port;
            if (server.contains(":")) {
                host = server.split(":")[0];
                port = Integer.valueOf(server.split(":")[1]);
            } else {
                host = server;
                port = ConfigurationProperties.DEFAULT_HOTROD_PORT;
            }
            configurationBuilder.addServer().host(host).port(port).pingOnStartup(false);
        }

        return new Site(servers,new RemoteCacheManager(configurationBuilder.build(),true),isLocal);
    }

    //
    // OBJECT METHODS
    //

    public Site(String name, RemoteCacheManager container, boolean isLocal) {
        this.name = name;
        this.isLocal = isLocal;
        this.container= container;
        synchronized(this.getClass()){
            if(isLocal && localSite==null)
                localSite = this;
        }
    }

    public Site(URL url, boolean isLocal) {
        name = url.toString();
        container = new RemoteCacheManager(url.getHost(), url.getPort(), true);
        this.isLocal = isLocal;
    }

    public Site(String name, URL url, boolean isLocal) {
        this.name = name;
        container = new RemoteCacheManager(url.getHost(), url.getPort(), true);
        this.isLocal = isLocal;
    }


    public String getName(){
        return name;
    }

    public RemoteCacheManager getManager(){
        return container;
    }

    public <K,V> SiteEnsembleCache<K,V> getCache(String name){
        return new SiteEnsembleCache<>(this,container.getCache(name));
    }

    public boolean isOwner(RemoteCache remoteCache){
        return container.equals(remoteCache.getRemoteCacheManager());
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
