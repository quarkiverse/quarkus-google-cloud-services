package io.quarkiverse.googlecloudservices.firebase.admin.runtime.authentication.http;

import io.quarkus.security.identity.request.BaseAuthenticationRequest;

/**
 * This class represents a Firebase authentication request, extending BaseAuthenticationRequest.
 * It contains a token used for authenticating with Firebase and indication if the request originated from
 * a (session) cookie.
 */
public class FirebaseAuthenticationRequest extends BaseAuthenticationRequest {

    private final String token;
    private final boolean cookie;

    /**
     * Creates a new FirebaseAuthenticationRequest object with the given token.
     *
     * @param token The authentication token to be used in the request.
     * @param cookie Whether the token was retrieved from a session cookie
     */
    public FirebaseAuthenticationRequest(String token, boolean cookie) {
        this.token = token;
        this.cookie = cookie;
    }

    /**
     * Retrieves the authentication token associated with this request.
     *
     * @return A String representing the authentication token.
     */
    public String getToken() {
        return token;
    }

    /**
     * Returns whether the token was retrieved from a session cookie.
     *
     * @return retrieved from cookie or not
     */
    public boolean isCookie() {
        return cookie;
    }
}
