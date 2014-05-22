package org.infinispan.ensemble.rest;

import org.infinispan.ensemble.EnsembleCache;
import org.jboss.logging.Logger;

import javax.activation.MimetypesFileTypeMap;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;


/*

<K,V> BasicCache<K,V> getCache()
<K,V> BasicCache<K,V> getCache(String cacheName)
<K,V> BasicCache<K,V> getCache(String cacheName, int replicationFactor)
<K,V> BasicCache<K,V> getCache(String cacheName, int replicationFactor, Consistency consistency)
<K,V> BasicCache<K,V> getCache(String cacheName, List<Site> sites, Consistency consistency)
boolean addSite(Site site)
Set<Site> sites()

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

    @GET
    @Path("getcache/{cache}/{repl}")
    @Produces("application/json")
    public EnsembleCache getCache(@PathParam("cache") String cache,
                              @PathParam("repl") String srepl) {
        String message = "GET /manager/getcache/" + cache + " called";
        logger.info(message);

        EnsembleCache ec;
        if (cache.length() == 0) {
            ec = context.getManager().getCache();
        } else {
            if (srepl.length() == 0) {
                ec = context.getManager().getCache(cache);
            } else {
                ec = context.getManager().getCache(cache, Integer.parseInt(srepl));
            }
        }

//        return Response.status(200).entity(ec).build();
        return ec;
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
}
