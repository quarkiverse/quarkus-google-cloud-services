package io.quarkiverse.googlecloudservices.storage.runtime;

import java.io.IOException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.google.auth.Credentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import io.quarkiverse.googlecloudservices.common.GcpBootstrapConfiguration;
import io.quarkiverse.googlecloudservices.common.GcpConfigHolder;

@ApplicationScoped
public class StorageProducer {

    @Inject
    Credentials googleCredentials;

    @Inject
    GcpConfigHolder gcpConfigHolder;

    @Inject
    StorageConfiguration storageConfiguration;

    @Produces
    @Singleton
    @Default
    public Storage storage() throws IOException {
        GcpBootstrapConfiguration gcpConfiguration = gcpConfigHolder.getBootstrapConfig();
        StorageOptions.Builder builder = StorageOptions.newBuilder()
                .setCredentials(googleCredentials)
                .setProjectId(gcpConfiguration.projectId().orElse(null));
        storageConfiguration.hostOverride().ifPresent(builder::setHost);
        return builder.build().getService();
    }

    public void close(@Disposes Storage storage) throws Exception {
        storage.close();
    }
}
