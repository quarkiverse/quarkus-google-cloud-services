package io.quarkiverse.googlecloudservices.firebase.admin.runtime.authentication.http;

import java.security.Principal;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

/**
 * This class is an implementation of IdentityProvider for FirebaseAuthenticationRequest.
 * It provides methods to authenticate a user using Firebase tokens.
 */
@ApplicationScoped
public class DefaultFirebaseIdentityProvider implements IdentityProvider<FirebaseAuthenticationRequest> {

    @Inject
    FirebaseAuth auth;

    /**
     * Retrieves the request type that this provider supports.
     *
     * @return The class of FirebaseAuthenticationRequest.
     */
    @Override
    public Class<FirebaseAuthenticationRequest> getRequestType() {
        return FirebaseAuthenticationRequest.class;
    }

    /**
     * Authenticates the provided FirebaseAuthenticationRequest using the FirebaseAuth instance.
     *
     * @param request The authentication request containing the Firebase token.
     * @param context The context of the authentication request.
     * @return A Uni of SecurityIdentity representing the authenticated user or an empty optional if authentication fails.
     */
    @Override
    public Uni<SecurityIdentity> authenticate(FirebaseAuthenticationRequest request, AuthenticationRequestContext context) {
        return Uni.createFrom()
                .future(auth.verifyIdTokenAsync(request.getToken()))
                .onItem().transformToUni(idToken -> {
                    // Authenticate the token and create a SecurityIdentity
                    SecurityIdentity identity = authenticate(idToken);

                    // If the token is invalid, return an empty optional
                    if (identity == null) {
                        return Uni.createFrom().optional(Optional.empty());
                    }

                    return Uni.createFrom().item(identity);
                });
    }

    /**
     * Authenticates the provided FirebaseToken and creates a SecurityIdentity.
     *
     * @param token The FirebaseToken to be authenticated.
     * @return A SecurityIdentity representing the authenticated user or null if authentication fails.
     */
    public static SecurityIdentity authenticate(FirebaseToken token) {
        var builder = QuarkusSecurityIdentity.builder()
                .setPrincipal(getPrincipal(token));

        return builder.build();
    }

    /**
     * Creates a FirebasePrincipal from the provided FirebaseToken.
     *
     * @param token The FirebaseToken to be used for creating the principal.
     * @return A FirebasePrincipal object representing the authenticated user.
     */
    public static Principal getPrincipal(FirebaseToken token) {
        return new FirebasePrincipal(token);
    }
}
