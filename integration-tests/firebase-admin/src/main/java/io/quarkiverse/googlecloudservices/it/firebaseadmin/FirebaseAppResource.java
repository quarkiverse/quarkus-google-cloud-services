package io.quarkiverse.googlecloudservices.it.firebaseadmin;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

@Path("/app")
public class FirebaseAppResource {

    @Inject
    FirebaseApp firebaseApp;

    @GET
    @Path("/options")
    @Produces(MediaType.APPLICATION_JSON)
    public FirebaseOptions getOptions() {
        return firebaseApp.getOptions();
    }

    @GET
    @Authenticated
    @Path("/secret-options")
    @Produces(MediaType.APPLICATION_JSON)
    public FirebaseOptions getSecretOptions() {
        return firebaseApp.getOptions();
    }

}
