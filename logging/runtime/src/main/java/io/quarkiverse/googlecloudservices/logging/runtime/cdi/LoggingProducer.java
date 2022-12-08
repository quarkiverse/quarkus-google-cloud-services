package io.quarkiverse.googlecloudservices.logging.runtime.cdi;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

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
        Logging log = LoggingOptions.getDefaultInstance().toBuilder()
                .setCredentials(googleCredentials)
                .setProjectId(projectId)
                .build()
                .getService();
        // check auto-flush and synchronicity
        loggingConfig.flushLevel.ifPresent(level -> log.setFlushSeverity(level.getSeverity()));
        loggingConfig.synchronicity.ifPresent(sync -> log.setWriteSynchronicity(sync));
        return log;
    }
}
