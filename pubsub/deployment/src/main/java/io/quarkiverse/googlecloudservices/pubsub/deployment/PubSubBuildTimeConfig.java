package io.quarkiverse.googlecloudservices.pubsub.deployment;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * Root configuration class for Google Cloud Pub/Sub that operates at build time.
 * This class provides a nested structure for configuration, including
 * a separate group for the development service configuration.
 */
@ConfigMapping(prefix = "quarkus.google.cloud.pubsub")
@ConfigRoot
public interface PubSubBuildTimeConfig {

    /**
     * Configuration for the Pub/Sub dev service.
     * These settings will be used when Pub/Sub service is being configured
     * for development purposes.
     */
    PubSubDevServiceConfig devservice();
}
