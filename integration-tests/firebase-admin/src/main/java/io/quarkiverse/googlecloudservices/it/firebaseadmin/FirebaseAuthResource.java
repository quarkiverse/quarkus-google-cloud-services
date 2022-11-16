package io.quarkiverse.googlecloudservices.it.firebaseadmin;

import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

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
