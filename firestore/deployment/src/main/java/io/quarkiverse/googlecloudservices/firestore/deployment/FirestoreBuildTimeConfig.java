package io.quarkiverse.googlecloudservices.firestore.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Root configuration class for Firestore that operates at build time.
 * This class provides a nested structure for configuration, including
 * a separate group for the development service configuration.
 */
@ConfigRoot(name = "google.cloud.firestore", phase = ConfigPhase.BUILD_TIME)
public class FirestoreBuildTimeConfig {

    /**
     * Configuration for the Firestore dev service.
     * These settings will be used when Firestore service is being configured
     * for development purposes.
     */
    @ConfigItem
    public FirestoreDevServiceConfig devservice;
}
