package io.quarkiverse.googlecloudservices.common;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

import io.quarkus.security.credential.Credential;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.SecurityIdentity;

/**
 * Shared helpers to create Google Cloud credentials from the extension configuration.
 * <p>
 * This class is not a CDI bean so it can also be used at bootstrap time, by config sources that run before the CDI
 * container is available.
 */
public final class GcpCredentials {

    private static final String CLOUD_OAUTH_SCOPE = "https://www.googleapis.com/auth/cloud-platform";

    private GcpCredentials() {
    }

    /**
     * Creates credentials from a service account file or a base64 encoded service account key.
     *
     * @return the credentials, or {@code null} when no service account is configured
     */
    public static Credentials fromServiceAccount(Optional<String> serviceAccountLocation,
            Optional<String> serviceAccountEncodedKey) throws IOException {
        if (serviceAccountLocation.isPresent()) {
            try (FileInputStream is = new FileInputStream(serviceAccountLocation.get())) {
                return GoogleCredentials.fromStream(is).createScoped(CLOUD_OAUTH_SCOPE);
            }
        }
        if (serviceAccountEncodedKey.isPresent()) {
            byte[] decodedKey = Base64.getDecoder().decode(serviceAccountEncodedKey.get());
            try (ByteArrayInputStream is = new ByteArrayInputStream(decodedKey)) {
                return GoogleCredentials.fromStream(is).createScoped(CLOUD_OAUTH_SCOPE);
            }
        }
        return null;
    }

    /**
     * Creates credentials from the bearer token of the given security identity.
     *
     * @return the credentials, or {@code null} when the identity does not have a bearer token
     */
    public static Credentials fromAccessToken(SecurityIdentity securityIdentity) {
        for (Credential credential : securityIdentity.getCredentials()) {
            if (credential instanceof TokenCredential) {
                TokenCredential tokenCredential = (TokenCredential) credential;
                if ("bearer".equals(tokenCredential.getType())) {
                    return GoogleCredentials
                            .create(new AccessToken(tokenCredential.getToken(), null))
                            .createScoped(CLOUD_OAUTH_SCOPE);
                }
            }
        }
        return null;
    }

    /**
     * Creates credentials from the Google Cloud application default credentials.
     */
    public static Credentials applicationDefault() throws IOException {
        return GoogleCredentials.getApplicationDefault().createScoped(CLOUD_OAUTH_SCOPE);
    }
}
