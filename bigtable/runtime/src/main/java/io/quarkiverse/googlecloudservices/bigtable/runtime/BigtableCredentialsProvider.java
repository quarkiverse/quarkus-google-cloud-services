package io.quarkiverse.googlecloudservices.bigtable.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.bigtable.data.v2.BigtableDataSettings;

@ApplicationScoped
public class BigtableCredentialsProvider {

    @Inject
    CredentialsProvider credentialsProvider;

    public void applyCredentials(BigtableDataSettings.Builder builder) {
        if (credentialsProvider != null) {
            builder.setCredentialsProvider(credentialsProvider);
        }
    }
}
