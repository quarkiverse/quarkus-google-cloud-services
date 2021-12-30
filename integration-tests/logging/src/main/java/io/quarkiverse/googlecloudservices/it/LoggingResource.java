package io.quarkiverse.googlecloudservices.it;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.logging.Logger;

@Path("/logging")
public class LoggingResource {

    @Inject
    Logger logger;

    @GET
    @Path("/{payload}")
    @Produces(MediaType.TEXT_PLAIN)
    public String tryLog(@PathParam("payload") String p) {
        logger.info("Hello " + p);
        return "Hello " + p;
    }
}
