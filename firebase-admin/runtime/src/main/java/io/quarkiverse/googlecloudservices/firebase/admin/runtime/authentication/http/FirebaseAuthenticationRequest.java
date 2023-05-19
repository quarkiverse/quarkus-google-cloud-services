package io.quarkiverse.googlecloudservices.firebase.admin.runtime.authentication.http;

import io.quarkus.security.identity.request.BaseAuthenticationRequest;

/**
 * This class represents a Firebase authentication request, extending BaseAuthenticationRequest.
 * It contains a token used for authenticating with Firebase.
 */
public class FirebaseAuthenticationRequest extends BaseAuthenticationRequest {

    private final String token;

    /**
     * Creates a new FirebaseAuthenticationRequest object with the given token.
     *
     * @param token The authentication token to be used in the request.
     */
    public FirebaseAuthenticationRequest(String token) {
        this.token = token;
    }

    /**
     * Retrieves the authentication token associated with this request.
     *
     * @return A String representing the authentication token.
     */
    public String getToken() {
        return token;
    }
}
