package org.infinispan.ensemble;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
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

@XmlRootElement(name="site")
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
    private URL url; // probably change to URL here
    private transient boolean isLocal;
    private transient RemoteCacheManager container;

    //
    // PUBLIC METHODS
    //

    public static RemoteCacheManager getRemoteManager(URL url) {
        // build or get a RemoteCacheManager for a given URL
        throw new UnsupportedOperationException();
    }

    public static boolean isLocal(URL url) {
        // true if URL refers to the local JVM
        throw new UnsupportedOperationException();
    }

    public Site(URL url) {
        this(url.toString(), getRemoteManager(url), isLocal(url), url);
    }

    public Site(String name, URL url) {
        this(name, getRemoteManager(url), isLocal(url), url);
    }

    public Site(String name, URL url, boolean isLocal) {
        this(name, getRemoteManager(url), isLocal, url);
    }

    public Site(String name, RemoteCacheManager container, boolean isLocal) {
        this(name, container, isLocal, null);
        try {
            this.url = new URL("hotrod://" + name + ":1234/example");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public Site(String name, RemoteCacheManager container, boolean isLocal, URL url) {
        this.name = name;
        this.isLocal = isLocal;
        this.container = container;
        this.url = url;

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

    @XmlElement(name="name")
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

    @XmlElement(name="url")
    public URL getUrl() {
        return url;
    }

    public void setUrl(String url) {
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void setUrl(URL url) {
        this.url = url;
    }
}
