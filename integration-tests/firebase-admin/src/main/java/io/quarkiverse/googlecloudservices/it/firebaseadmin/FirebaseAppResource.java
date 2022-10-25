package io.quarkiverse.googlecloudservices.it.firebaseadmin;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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

}
