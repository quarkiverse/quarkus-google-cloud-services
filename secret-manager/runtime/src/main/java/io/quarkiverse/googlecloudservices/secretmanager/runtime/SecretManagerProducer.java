package io.quarkiverse.googlecloudservices.secretmanager.runtime;

import java.io.IOException;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.auth.oauth2.GoogleCredentials;
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
    GoogleCredentials googleCredentials;

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

        builder.setQuotaProjectId(gcpConfiguration.projectId);

        return SecretManagerServiceClient.create(builder.build());
    }
}
