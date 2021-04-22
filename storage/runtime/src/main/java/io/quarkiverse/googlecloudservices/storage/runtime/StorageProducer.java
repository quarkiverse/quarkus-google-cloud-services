package io.quarkiverse.googlecloudservices.storage.runtime;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import io.quarkiverse.googlecloudservices.common.GcpConfiguration;

@ApplicationScoped
public class StorageProducer {

    @Inject
    GoogleCredentials googleCredentials;

    @Inject
    GcpConfiguration gcpConfiguration;

    @Inject
    StorageConfiguration storageConfiguration;

    @Produces
    @Singleton
    @Default
    public Storage storage() throws IOException {
        StorageOptions.Builder builder = StorageOptions.newBuilder()
                .setCredentials(googleCredentials)
                .setProjectId(gcpConfiguration.projectId);
        storageConfiguration.hostOverride.ifPresent(builder::setHost);
        return builder.build().getService();
    }
}
