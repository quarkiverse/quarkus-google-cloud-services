package io.quarkiverse.googlecloudservices.firestore.deployment;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * Root configuration class for Firestore that operates at build time.
 * This class provides a nested structure for configuration, including
 * a separate group for the development service configuration.
 */
@ConfigMapping(prefix = "quarkus.google.cloud.firestore")
@ConfigRoot
public interface FirestoreBuildTimeConfig {

    /**
     * Configuration for the Firestore dev service.
     * These settings will be used when Firestore service is being configured
     * for development purposes.
     */
    FirestoreDevServiceConfig devservice();
}
