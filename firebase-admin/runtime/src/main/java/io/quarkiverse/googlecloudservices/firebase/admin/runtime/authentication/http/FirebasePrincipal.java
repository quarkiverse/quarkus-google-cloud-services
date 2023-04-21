package io.quarkiverse.googlecloudservices.firebase.admin.runtime.authentication.http;

import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.google.firebase.auth.FirebaseToken;

/**
 * This class represents a Firebase principal that implements JsonWebToken interface.
 * It provides methods to access the properties of the underlying Firebase token.
 */
public class FirebasePrincipal implements JsonWebToken {

    private final FirebaseToken token;
    private final Map<String, Object> claims;

    /**
     * Creates a new FirebasePrincipal object with the given FirebaseToken.
     *
     * @param token The FirebaseToken to be wrapped by this principal.
     */
    public FirebasePrincipal(FirebaseToken token) {
        this.token = token;
        this.claims = token.getClaims();
    }

    /**
     * Retrieves the name associated with the FirebaseToken.
     *
     * @return A String representing the name associated with the token.
     */
    @Override
    public String getName() {
        return token.getName();
    }

    /**
     * Retrieves a set of claim names available in the FirebaseToken.
     *
     * @return A Set of Strings representing the claim names available in the token.
     */
    @Override
    public Set<String> getClaimNames() {
        return claims.keySet();
    }

    /**
     * Retrieves the value of the specified claim from the FirebaseToken.
     *
     * @param claimName The name of the claim to be retrieved.
     * @return The value of the specified claim or null if not found.
     */
    @Override
    public <T> T getClaim(String claimName) {
        return (T) claims.get(claimName);
    }

    /**
     * Retrieves the underlying FirebaseToken.
     *
     * @return The FirebaseToken wrapped by this principal.
     */
    public FirebaseToken getToken() {
        return token;
    }
}
