package org.infinispan.ensemble.rest;

import org.jboss.logging.Logger;

import javax.activation.MimetypesFileTypeMap;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/*

V get(Object key)
V put(K key, V value)
int size()
boolean isEmpty()
String getName()
void start()
void stop()

*/

@Path("ensemble")
public class EnsembleCacheRestService {

    private static EnsembleCacheRestContext context = null;
    private static Logger logger;

    public static void setContext(EnsembleCacheRestContext ctx) {
        context = ctx;
        logger = Logger.getLogger(EnsembleCacheRestService.class);
    }

    public EnsembleCacheRestService() {
        assert context != null;
    }


    @GET
    @Path("{cache}/get")
    public Response  get(@PathParam("cache") String cache){
        String message = "GET /ensemble/get/" + cache + " called";
        logger.info(message);
        message += "\n";
        return Response.status(200).entity(message).build();
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
