package io.quarkiverse.googlecloudservices.common;

import java.io.FileInputStream;
import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

import io.quarkus.security.credential.Credential;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.SecurityIdentity;

@ApplicationScoped
public class GcpCredentialProducer {

    private static final String CLOUD_OAUTH_SCOPE = "https://www.googleapis.com/auth/cloud-platform";

    @Inject
    GcpConfiguration gcpConfiguration;

    @Inject
    Instance<SecurityIdentity> securityIdentity;

    @PostConstruct
    public void verifySecurityIdentity() {
        if (securityIdentity.isResolvable() && securityIdentity.isAmbiguous()) {
            throw new IllegalStateException("Multiple " + SecurityIdentity.class + " beans registered");
        }
    }

    @Produces
    @Singleton
    @Default
    public GoogleCredentials googleCredential() throws IOException {
        if (gcpConfiguration.serviceAccountLocation.isPresent()) {
            try (FileInputStream is = new FileInputStream(gcpConfiguration.serviceAccountLocation.get())) {
                return GoogleCredentials.fromStream(is).createScoped(CLOUD_OAUTH_SCOPE);
            }
        } else if (gcpConfiguration.accessTokenEnabled && securityIdentity.isResolvable()
                && !securityIdentity.get().isAnonymous()) {
            for (Credential cred : securityIdentity.get().getCredentials()) {
                if (cred instanceof TokenCredential && "bearer".equals(((TokenCredential) cred).getType())) {
                    return GoogleCredentials
                            .create(new AccessToken(((TokenCredential) cred).getToken(), null))
                            .createScoped(CLOUD_OAUTH_SCOPE);
                }
            }
        }

        return GoogleCredentials.getApplicationDefault().createScoped(CLOUD_OAUTH_SCOPE);
    }
}
