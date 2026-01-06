package io.quarkiverse.googlecloudservices.spanner.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.google.auth.Credentials;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;

import io.quarkiverse.googlecloudservices.common.GcpBootstrapConfiguration;
import io.quarkiverse.googlecloudservices.common.GcpConfigHolder;

@ApplicationScoped
public class SpannerProducer {
    @Inject
    Instance<Credentials> googleCredentials;

    @Inject
    GcpConfigHolder gcpConfigHolder;

    @Inject
    SpannerConfiguration spannerConfiguration;

    @Produces
    @Singleton
    @Default
    public Spanner storage() {
        GcpBootstrapConfiguration gcpConfiguration = gcpConfigHolder.getBootstrapConfig();
        SpannerOptions.Builder builder = SpannerOptions.newBuilder()
                .setProjectId(gcpConfiguration.projectId().orElse(null));

        // SpannerOptions automatically uses NoCredentials in case an emulator host is set.
        if (spannerConfiguration.emulatorHost().isEmpty()) {
            builder.setCredentials(googleCredentials.get());
        }

        spannerConfiguration.emulatorHost().ifPresent(builder::setEmulatorHost);
        spannerConfiguration.databaseRole().ifPresent(builder::setDatabaseRole);
        return builder.build().getService();
    }

    public void close(@Disposes Spanner spanner) {
        spanner.close();
    }
}
