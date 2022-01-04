package io.quarkiverse.googlecloudservices.logging.runtime.cdi;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;

import io.quarkiverse.googlecloudservices.common.GcpBootstrapConfiguration;
import io.quarkiverse.googlecloudservices.common.GcpConfigHolder;
import io.quarkiverse.googlecloudservices.logging.runtime.LoggingConfiguration;
import io.quarkus.arc.Unremovable;

/**
 * Producer for a Google logging instance.
 */
@Singleton
public class LoggingProducer {

    @Inject
    GoogleCredentials googleCredentials;

    @Inject
    GcpConfigHolder gcpConfigHolder;

    @Inject
    LoggingConfiguration loggingConfig;

    @Default
    @Produces
    @Singleton
    @Unremovable
    public Logging create() {
        GcpBootstrapConfiguration gcpConfiguration = gcpConfigHolder.getBootstrapConfig();
        String projectId = gcpConfiguration.projectId.orElse(null);
        return LoggingOptions.getDefaultInstance().toBuilder()
                .setCredentials(googleCredentials)
                .setProjectId(projectId)
                .build()
                .getService();
    }
}
