package io.quarkiverse.googlecloudservices.it.firebaseadmin;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import io.quarkus.security.Authenticated;

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

    @GET
    @RolesAllowed({ "admin" })
    @Path("/admin-options")
    @Produces(MediaType.APPLICATION_JSON)
    public FirebaseOptions getAdminOptions() {
        return firebaseApp.getOptions();
    }

}
