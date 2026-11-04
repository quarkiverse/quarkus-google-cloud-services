package io.quarkiverse.googlecloudservices.secretmanager.runtime;

import java.io.IOException;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.google.auth.Credentials;

import io.quarkiverse.googlecloudservices.common.GcpBootstrapConfiguration;
import io.quarkiverse.googlecloudservices.common.GcpConfigHolder;
import io.quarkiverse.googlecloudservices.common.GcpCredentialsFactory;

/**
 * Produces the credentials used by the Secret Manager client, following the Secret Manager specific authentication
 * configuration.
 */
@Singleton
public class SecretManagerCredentialProducer {

    @Inject
    GcpCredentialsFactory credentialsFactory;

    @Inject
    GcpConfigHolder gcpConfigHolder;

    @Inject
    SecretManagerConfiguration configuration;

    @Produces
    @Singleton
    @SecretManagerCredentials
    public Credentials secretManagerCredentials() throws IOException {
        GcpBootstrapConfiguration globalConfiguration = gcpConfigHolder.getBootstrapConfig();
        return credentialsFactory.create(
                configuration.effectiveServiceAccountLocation(globalConfiguration),
                configuration.effectiveServiceAccountEncodedKey(globalConfiguration),
                configuration.effectiveAccessTokenEnabled(globalConfiguration));
    }
}
