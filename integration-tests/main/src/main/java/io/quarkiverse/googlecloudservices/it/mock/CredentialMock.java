package io.quarkiverse.googlecloudservices.it.mock;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import com.google.auth.Credentials;
import com.google.cloud.NoCredentials;

@ApplicationScoped
public class CredentialMock {

    @Produces
    @Singleton
    @Alternative
    @Priority(1)
    public Credentials googleCredential() {
        return NoCredentials.getInstance();
    }

}
