package io.quarkiverse.googlecloudservices.secretmanager.runtime;

import java.io.IOException;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.google.auth.Credentials;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretManagerServiceSettings;

import io.quarkiverse.googlecloudservices.common.GcpBootstrapConfiguration;
import io.quarkiverse.googlecloudservices.common.GcpConfigHolder;
import io.quarkus.arc.Unremovable;

/**
 * Producer for the Google Cloud Secret Manager service.
 */
@Singleton
public class SecretManagerProducer {

    @Inject
    Credentials googleCredentials;

    @Inject
    GcpConfigHolder gcpConfigHolder;

    @Produces
    @Singleton
    @Default
    @Unremovable
    public SecretManagerServiceClient secretManagerClient() throws IOException {
        GcpBootstrapConfiguration gcpConfiguration = gcpConfigHolder.getBootstrapConfig();
        SecretManagerServiceSettings.Builder builder = SecretManagerServiceSettings.newBuilder()
                .setCredentialsProvider(() -> googleCredentials);

        builder.setQuotaProjectId(gcpConfiguration.projectId().orElse(null));

        return SecretManagerServiceClient.create(builder.build());
    }

    public void close(@Disposes SecretManagerServiceClient secretManagerClient) {
        secretManagerClient.close();
    }
}
