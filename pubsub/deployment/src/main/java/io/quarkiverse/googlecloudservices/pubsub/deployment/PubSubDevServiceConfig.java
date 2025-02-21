package io.quarkiverse.googlecloudservices.pubsub.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration group for the Pub/Sub. This class holds all the configuration properties
 * related to the Google Cloud Pub/Sub service for development environments.
 * <p>
 * Here is an example of how to configure these properties:
 * <p>
 *
 * <pre>
 * quarkus.google.cloud.pubsub.devservice.enabled = true
 * quarkus.google.cloud.pubsub.devservice.image-name = gcr.io/google.com/cloudsdktool/google-cloud-cli # optional
 * quarkus.google.cloud.pubsub.devservice.emulatorPort = 8085 # optional
 * </pre>
 */
@ConfigMapping(prefix = "quarkus.google.cloud.pubsub.devservice")
@ConfigRoot
public interface PubSubDevServiceConfig {

    /**
     * Indicates whether the Pub/Sub service should be enabled or not.
     * The default value is 'false'.
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * Sets the Docker image name for the Google Cloud SDK.
     * This image is used to emulate the Pub/Sub service in the development environment.
     * The default value is 'gcr.io/google.com/cloudsdktool/google-cloud-cli'.
     */
    @WithDefault("gcr.io/google.com/cloudsdktool/google-cloud-cli")
    String imageName();

    /**
     * Specifies the emulatorPort on which the Pub/Sub service should run in the development environment.
     */
    Optional<Integer> emulatorPort();
}
