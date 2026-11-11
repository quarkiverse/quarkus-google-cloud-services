package io.quarkiverse.googlecloudservices.it.mock;

import java.util.Optional;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.NoCredentials;

@ApplicationScoped
public class CredentialMock {

    @ConfigProperty(name = "integration-tests.credentials.access-token")
    Optional<String> accessToken;

    @Produces
    @Singleton
    @Alternative
    @Priority(1)
    public Credentials googleCredential() {
        return accessToken
                .filter(token -> !token.isEmpty())
                .<Credentials> map(token -> GoogleCredentials.create(new AccessToken(token, null)))
                .orElseGet(NoCredentials::getInstance);
    }

}
