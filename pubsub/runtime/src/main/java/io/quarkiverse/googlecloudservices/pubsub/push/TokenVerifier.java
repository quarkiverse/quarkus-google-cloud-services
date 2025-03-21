package io.quarkiverse.googlecloudservices.pubsub.push;

import java.io.IOException;
import java.security.GeneralSecurityException;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

/**
 * Wrapper interface around {@link com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier}. This class
 * is hard to mock otherwise and by itself doesn't really offer any options to handle test-scenario's where the
 * send JWT is fake.
 * <br>
 * The library is in maintenance mode, so we don't expect any changes to resolve this.
 */
public interface TokenVerifier {

    /**
     * Verify the token and return the parsed token. Returns null if the token is invalid.
     *
     * @see com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier#verify(String)
     * @param token The token
     * @return The parsed token.
     */
    GoogleIdToken verify(String token) throws GeneralSecurityException, IOException;
}
