package org.infinispan.ensemble.rest;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.ensemble.EnsembleCache;
import org.infinispan.ensemble.EnsembleCacheManager;
import org.infinispan.ensemble.Site;
import org.jboss.logging.Logger;

import javax.activation.MimetypesFileTypeMap;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;


/*

<K,V> BasicCache<K,V> getCache(String cacheName, List<Site> sites, Consistency consistency)
boolean addSite(Site site)
Set<Site> sites()



Create = PUT with a new URI
         POST to a base URI returning a newly created URI
Read   = GET
Update = PUT with an existing URI
Delete = DELETE


*/



@Path("manager")
public class EnsembleCacheManagerRestService {

    private static EnsembleCacheRestContext context = null;
    private static Logger logger;

    public static void setContext(EnsembleCacheRestContext ctx) {
        context = ctx;
        logger = Logger.getLogger(EnsembleCacheRestService.class);
    }

    public EnsembleCacheManagerRestService() {
        assert context != null;
    }

    @POST
    @Path("cache")
    @Produces("application/json")
    public EnsembleCache postCache() {
        String message = "POST /manager/cache called";
        logger.info(message);

        EnsembleCache ec = context.getEnsembleManager().getCache();
        return ec;
    }

    @POST
    @Path("cache/{cache}")
    @Produces("application/json")
    public EnsembleCache postCache(@PathParam("cache") String cache) {
        String message = "POST /manager/cache/" + cache + " called";
        logger.info(message);

        EnsembleCache ec = null;
        if (cache.length() == 0) {
            // report error
        } else {
            ec = context.getEnsembleManager().getCache(cache);
        }
        return ec;
    }

    @POST
    @Path("cache/{cache}/{repl}")
    @Produces("application/json")
    public EnsembleCache postCache(@PathParam("cache") String cache,
                                   @PathParam("repl") String srepl) {
        String message = "POST /manager/cache/" + cache + "/" + srepl + " called";
        logger.info(message);

        EnsembleCache ec = null;
        if (cache.length() == 0 || srepl.length() == 0) {
            // report error
        } else {
            ec = context.getEnsembleManager().getCache(cache, Integer.parseInt(srepl));
        }
        return ec;
    }

    @POST
    @Path("cache/{cache}/{repl}/{consis}")
    @Produces("application/json")
    public EnsembleCache postCache(@PathParam("cache") String cache,
                                   @PathParam("repl") String srepl,
                                   @PathParam("consis") String sconsis) {
        String message = "POST /manager/cache/" + cache + "/" + srepl + "/" + sconsis + " called";
        logger.info(message);

        EnsembleCache ec = null;
        if (cache.length() == 0 || srepl.length() == 0 || sconsis.length() == 0) {
            // report error
        } else {
            ec = context.getEnsembleManager().getCache(cache, Integer.parseInt(srepl),
                    EnsembleCacheManager.Consistency.valueOf(sconsis));
        }
        return ec;
    }

    @POST
    @Path("cache/{cache}/{consis}/explicit")
    @Produces("application/json")
    public EnsembleCache postCache2(@PathParam("cache") String cache,
                                   @PathParam("consis") String sconsis,
                                   @QueryParam("sites") String sites) {
        String message = "POST /manager/cache/" + cache + "/" + sconsis + "/sites/" + sites + " called";
        logger.info(message);

        EnsembleCache ec = null;
        if (cache.length() == 0 || sites.length() == 0 || sconsis.length() == 0) {
            // report error
        } else {
            List<Site> lsites = new LinkedList<Site>();
            for (String s: sites.split("\\+")) {
                try {
                    lsites.add(new Site(new URL(s)));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
            ec = context.getEnsembleManager().getCache(cache, lsites,
                    EnsembleCacheManager.Consistency.valueOf(sconsis));
        }
        return ec;
    }

    @POST
    @Path("site/{site}/{url:.*}")
    @Produces("application/json")
    public Site postSite(@PathParam("site") String site,
                                  @PathParam("url") String surl) {
        String message = "POST /manager/site/" + site + "/" + surl + " called";
        logger.info(message);

        URL url = null;
        try {
            Site sobj = new Site(new URL(surl));
            context.getEnsembleManager().addSite(sobj);
            return sobj;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // shuld return error
        return null;
    }

    @GET
    @Path("sites")
    @Produces("application/json")
    public Collection<Site> getSites() {
        String message = "GET /manager/sites called";
        logger.info(message);

        Collection<Site> sites = context.getEnsembleManager().sites();

        return sites;
    }


    /*****************************************************/
    @PUT
    @Path("{path}")
    public Response _put(@PathParam("path") String path) {
        String message = "unknown PUT path " + path;
        logger.error(message);
        message += "\n";
        return Response.status(404).entity(message).build();
    }

    @GET
    @Path("{path}")
    public Response _get(@PathParam("path") String path) {
        String message = "unknown GET path " + path;
        logger.error(message);
        message += "\n";
        return Response.status(404).entity(message).build();
    }

    @POST
    @Path("{path}")
    public Response _post(@PathParam("path") String path) {
        String message = "unknown POST path " + path;
        logger.error(message);
        message += "\n";
        return Response.status(404).entity(message).build();
    }

    @DELETE
    @Path("{path}")
    public Response _delete(@PathParam("path") String path) {
        String message = "unknown DELETE path " + path;
        logger.error(message);
        message += "\n";
        return Response.status(404).entity(message).build();
    }
}
