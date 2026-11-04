package io.quarkiverse.googlecloudservices.common;

import java.io.IOException;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.google.auth.Credentials;

@Singleton
public class GcpCredentialProducer {

    @Inject
    GcpCredentialsFactory credentialsFactory;

    @Inject
    GcpConfigHolder gcpConfigHolder;

    @Produces
    @Singleton
    @Default
    public Credentials googleCredential() throws IOException {
        return credentialsFactory.create(gcpConfigHolder.getBootstrapConfig());
    }
}
