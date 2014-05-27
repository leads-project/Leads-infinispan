package org.infinispan.ensemble.rest;

import org.jboss.logging.Logger;

import javax.activation.MimetypesFileTypeMap;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by pasin on 21/05/14.
 */

@Path("/")
public class RootRestService {

    private static Logger logger = null;

    public static void setContext(EnsembleCacheRestContext ctx) {
    }

    public RootRestService() {
        if (logger == null)
            logger = Logger.getLogger(EnsembleCacheRestService.class);
    }

    @GET
    @Path("{file}")
    public Response get(@PathParam("file") String file) throws URISyntaxException {

        URL indexURL = getClass().getClassLoader().getResource(file);
        if (indexURL != null) {
            File f = new File(indexURL.toURI());

            String mt = new MimetypesFileTypeMap().getContentType(f);
            return Response.ok(f, mt).build();
        } else
            return Response.status(404).entity("unknown path " + file + "\n").build();
    }

    @POST
    @Path("{file}")
    public Response put(@PathParam("file") String file) {
        return Response.status(403).entity("forbidden path " + file + "\n").build();
    }
}
