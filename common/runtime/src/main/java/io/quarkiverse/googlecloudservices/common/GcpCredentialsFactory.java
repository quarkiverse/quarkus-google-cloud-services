package io.quarkiverse.googlecloudservices.common;

import java.io.IOException;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.google.auth.Credentials;

import io.quarkus.security.identity.SecurityIdentity;

/**
 * Creates {@link Credentials} following the authentication chain configured for a given service.
 * <p>
 * A service can override the global configuration by providing its own service account and/or access token
 * configuration. The resolution order is:
 * <ol>
 * <li>service account file</li>
 * <li>base64 encoded service account key</li>
 * <li>access token of the current user (when enabled)</li>
 * <li>Google Cloud application default credentials</li>
 * </ol>
 */
@Singleton
public class GcpCredentialsFactory {

    @Inject
    Instance<SecurityIdentity> securityIdentity;

    @PostConstruct
    public void verifySecurityIdentity() {
        if (securityIdentity.isResolvable() && securityIdentity.isAmbiguous()) {
            throw new IllegalStateException("Multiple " + SecurityIdentity.class + " beans registered");
        }
    }

    /**
     * Creates credentials using the global Google Cloud configuration.
     */
    public Credentials create(GcpBootstrapConfiguration configuration) throws IOException {
        return create(configuration.serviceAccountLocation(), configuration.serviceAccountEncodedKey(),
                configuration.accessTokenEnabled());
    }

    /**
     * Creates credentials using the given configuration.
     *
     * @param serviceAccountLocation the service account file location, if any
     * @param serviceAccountEncodedKey the base64 encoded service account key, if any
     * @param accessTokenEnabled whether the access token of the current user can be used
     */
    public Credentials create(Optional<String> serviceAccountLocation,
            Optional<String> serviceAccountEncodedKey,
            boolean accessTokenEnabled) throws IOException {
        Credentials serviceAccountCredentials = GcpCredentials.fromServiceAccount(serviceAccountLocation,
                serviceAccountEncodedKey);
        if (serviceAccountCredentials != null) {
            return serviceAccountCredentials;
        }

        if (accessTokenEnabled && securityIdentity.isResolvable()) {
            SecurityIdentity identity = securityIdentity.get();
            if (!isAnonymous(identity)) {
                Credentials accessTokenCredentials = GcpCredentials.fromAccessToken(identity);
                if (accessTokenCredentials != null) {
                    return accessTokenCredentials;
                }
            }
        }

        return applicationDefault();
    }

    Credentials applicationDefault() throws IOException {
        return GcpCredentials.applicationDefault();
    }

    private boolean isAnonymous(SecurityIdentity securityIdentity) {
        try {
            return securityIdentity.isAnonymous();
        } catch (ContextNotActiveException e) {
            // this is less than ideal but currently when the credential is returned out of a request context
            // (for eg at configuration time for a config source) this exception can arise.
            return true;
        }
    }
}
