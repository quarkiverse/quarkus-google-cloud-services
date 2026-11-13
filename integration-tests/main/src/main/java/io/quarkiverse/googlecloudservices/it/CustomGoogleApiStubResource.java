package io.quarkiverse.googlecloudservices.it;

import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Minimal stub of a Google API endpoint. It echoes the request path and the headers used to
 * authenticate and identify the project, so tests can assert on what the custom client sent.
 */
@Path("/custom-api-stub")
public class CustomGoogleApiStubResource {

    @GET
    @Path("{path: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> echo(
            @PathParam("path") String path,
            @HeaderParam("Authorization") String authorization,
            @HeaderParam("X-Project-Id") String projectId) {
        return Map.of(
                "path", path,
                "authorization", authorization == null ? "" : authorization,
                "projectId", projectId == null ? "" : projectId);
    }
}
