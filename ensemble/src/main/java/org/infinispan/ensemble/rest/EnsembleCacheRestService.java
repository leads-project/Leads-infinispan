package org.infinispan.ensemble.rest;

import org.jboss.logging.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/*

Create = PUT with a new URI
         POST to a base URI returning a newly created URI
Read   = GET
Update = PUT with an existing URI
Delete = DELETE


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
    @Path("{cache}/data/{key}")
    @Produces("application/json")
    public Response getData(@PathParam("cache") String cache,
                        @PathParam("key") String key) {
        String message = "GET /ensemble/" + cache + "/data/" + key + " called";
        logger.info(message);
        message += "\n";
        // Cache c = context.getCache(cache);
        // value = c.get(key);
        // ret = context.encodeValue(value);
        return Response.status(200).entity(message).build();
    }

    @PUT
    @Path("{cache}/data/{key}")
    @Consumes("application/json")
    public Response putData(@PathParam("cache") String cache,
                        @PathParam("key") String key,
                        String body) {
        String message = "PUT /ensemble/" + cache + "/data/" + key + " = [" + body + "] called";
        logger.info(message);
        message += "\n";
        // Cache c = context.getCache(cache);
        // value = context.decodeValue(body);
        // ret = c.put(key, value);
        return Response.status(200).entity(message).build();
    }

    @GET
    @Path("{cache}/size")
    @Produces("application/json")
    public Response getSize(@PathParam("cache") String cache) {
        String message = "GET /ensemble/" + cache + "/size called";
        logger.info(message);
        message += "\n";
        // Cache c = context.getCache(cache);
        // value = c.size();
        // ret = context.encodeValue(value);
        return Response.status(200).entity(message).build();
    }

    @GET
    @Path("{cache}/name")
    @Produces("application/json")
    public Response getName(@PathParam("cache") String cache) {
        String message = "GET /ensemble/" + cache + "/name called";
        logger.info(message);
        message += "\n";
        // Cache c = context.getCache(cache);
        // value = c.getName();
        // ret = context.encodeValue(value);
        return Response.status(200).entity(message).build();
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
