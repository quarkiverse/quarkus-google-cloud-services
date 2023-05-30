package io.quarkiverse.googlecloudservices.pubsub.deployement;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Root configuration class for Google Cloud Pub/Sub that operates at build time.
 * This class provides a nested structure for configuration, including
 * a separate group for the development service configuration.
 */
@ConfigRoot(name = "google.cloud.pubsub", phase = ConfigPhase.BUILD_TIME)
public class PubSubBuildTimeConfig {

    /**
     * Configuration for the Pub/Sub development service.
     * These settings will be used when Pub/Sub service is being configured
     * for development purposes.
     */
    @ConfigItem
    public PubSubDevServiceConfig devservice;
}
