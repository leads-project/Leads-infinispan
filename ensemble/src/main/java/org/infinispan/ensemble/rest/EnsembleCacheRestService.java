package org.infinispan.ensemble.rest;

import org.jboss.logging.Logger;

import javax.activation.MimetypesFileTypeMap;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by pasin on 21/05/14.
 */

@Path("/")
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
    @Path("/index.html")
    @Produces("text/html")
    public Response index() throws URISyntaxException {

        URL indexURL = getClass().getClassLoader().getResource("index.html");
        File f =  new File(indexURL.toURI());

        String mt = new MimetypesFileTypeMap().getContentType(f);
        return Response.ok(f, mt).build();

    }

    @GET
    @Path("/manager")
    public Response  GETmanager() {
        logger.info("GET /manager");

        return Response.status(200).entity("HTTP GET /manager method called").build();

    }

    @GET
    @Path("/cache")
    public Response  GETcache(){
        logger.info("GET /cache");

        return Response.status(200).entity("HTTP GET /cache method called").build();

    }
}
