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
 * quarkus.google.cloud.pubsub.devservice.shared = true # optional
 * quarkus.google.cloud.pubsub.devservice.service-name = pubsub # optional
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
     * The default value is 'gcr.io/google.com/cloudsdktool/google-cloud-cli:emulators'.
     */
    @WithDefault("gcr.io/google.com/cloudsdktool/google-cloud-cli:emulators")
    String imageName();

    /**
     * Specifies the emulatorPort on which the Pub/Sub service should run in the development environment.
     */
    Optional<Integer> emulatorPort();

    /**
     * Indicates if the Pub/Sub emulator managed by Dev Services is shared.
     * When shared, Quarkus looks for running containers using label-based service discovery.
     * If a matching container is found, it is used, and so a second one is not started.
     * Otherwise, Dev Services starts a new container.
     * <p>
     * The discovery uses the {@code quarkus-dev-service-google-cloud-pubsub} label.
     * The value is configured using the {@code service-name} property.
     * <p>
     * Container sharing is only used in dev mode.
     */
    @WithDefault("true")
    boolean shared();

    /**
     * The value of the {@code quarkus-dev-service-google-cloud-pubsub} label attached to the started container.
     * This property is used when {@code shared} is set to {@code true}.
     * In this case, before starting a container, Dev Services looks for a container with the
     * {@code quarkus-dev-service-google-cloud-pubsub} label set to the configured value.
     * If found, it will use this container instead of starting a new one.
     * Otherwise, it starts a new container with the {@code quarkus-dev-service-google-cloud-pubsub} label set to the
     * specified value.
     */
    @WithDefault("pubsub")
    String serviceName();
}
