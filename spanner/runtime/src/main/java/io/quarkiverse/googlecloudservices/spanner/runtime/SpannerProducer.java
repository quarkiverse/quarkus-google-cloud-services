package io.quarkiverse.googlecloudservices.spanner.runtime;

import java.io.IOException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;

import io.quarkiverse.googlecloudservices.common.GcpBootstrapConfiguration;
import io.quarkiverse.googlecloudservices.common.GcpConfigHolder;

@ApplicationScoped
public class SpannerProducer {
    @Inject
    GoogleCredentials googleCredentials;

    @Inject
    GcpConfigHolder gcpConfigHolder;

    @Inject
    SpannerConfiguration spannerConfiguration;

    @Produces
    @Singleton
    @Default
    public Spanner storage() throws IOException {
        GcpBootstrapConfiguration gcpConfiguration = gcpConfigHolder.getBootstrapConfig();
        SpannerOptions.Builder builder = SpannerOptions.newBuilder().setCredentials(googleCredentials)
                .setProjectId(gcpConfiguration.projectId.orElse(null));
        spannerConfiguration.emulatorHost.ifPresent(builder::setEmulatorHost);
        return builder.build().getService();
    }
}
