package io.quarkiverse.googlecloudservices.it.firebaseadmin;

import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;

@Path("/auth")
public class FirebaseAuthResource {

    @Inject
    FirebaseAuth firebaseAuth;

    @GET
    @Path("/users/{uid}")
    @Produces(MediaType.APPLICATION_JSON)
    public UserRecord getUserById(@PathParam("uid") String uid) throws FirebaseAuthException {
        return firebaseAuth.getUser(uid);
    }

    @POST
    @Path("/users/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public UserRecord createUser(@QueryParam("uid") String uid, @QueryParam("email") String email,
            @QueryParam("displayName") String displayName)
            throws FirebaseAuthException {
        return firebaseAuth.createUser(new UserRecord.CreateRequest()
                .setUid(uid != null ? uid : UUID.randomUUID().toString())
                .setEmail(email)
                .setDisplayName(displayName));
    }

    @PUT
    @Path("/users/{uid}/claims")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void setCustomUserClaims(@PathParam("uid") String uid, Map<String, Object> customClaims)
            throws FirebaseAuthException {
        firebaseAuth.setCustomUserClaims(uid, customClaims);
    }

}
