package io.quarkiverse.googlecloudservices.spanner.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration group for the Spanner. This class holds all the configuration properties
 * related to the Google Cloud Spanner service for development environments.
 * <p>
 * Here is an example of how to configure these properties:
 * <p>
 *
 * <pre>
 * quarkus.google.cloud.spanner.devservice.enabled = true
 * quarkus.google.cloud.spanner.devservice.image-name = gcr.io/cloud-spanner-emulator/emulator:1.5.9 # optional
 * quarkus.google.cloud.spanner.devservice.emulatorPort = 8085 # optional
 * quarkus.google.cloud.spanner.devservice.shared = true # optional
 * quarkus.google.cloud.spanner.devservice.service-name = spanner # optional
 * </pre>
 */
@ConfigMapping(prefix = "quarkus.google.cloud.spanner.devservice")
@ConfigGroup
public interface SpannerDevServiceConfig {

    /**
     * Indicates whether the Spanner service should be enabled or not.
     * The default value is 'false'.
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * Sets the Docker image name for the Google Cloud SDK.
     * This image is used to emulate the Spanner service in the development environment.
     * The default value is 'gcr.io/cloud-spanner-emulator/emulator'.
     */
    @WithDefault("gcr.io/cloud-spanner-emulator/emulator")
    String imageName();

    /**
     * Specifies the emulatorPort on which the HTTP endpoint for the Spanner service should run in the development environment.
     */
    Optional<Integer> httpPort();

    /**
     * Specifies the emulatorPort on which the GRPC endpoint for the Spanner service should run in the development environment.
     */
    Optional<Integer> grpcPort();

    /**
     * Indicates if the Spanner emulator managed by Dev Services is shared.
     * When shared, Quarkus looks for running containers using label-based service discovery.
     * If a matching container is found, it is used, and so a second one is not started.
     * Otherwise, Dev Services starts a new container.
     * <p>
     * The discovery uses the {@code quarkus-dev-service-google-cloud-spanner} label.
     * The value is configured using the {@code service-name} property.
     * <p>
     * Container sharing is only used in dev mode.
     */
    @WithDefault("true")
    boolean shared();

    /**
     * The value of the {@code quarkus-dev-service-google-cloud-spanner} label attached to the started container.
     * This property is used when {@code shared} is set to {@code true}.
     * In this case, before starting a container, Dev Services looks for a container with the
     * {@code quarkus-dev-service-google-cloud-spanner} label set to the configured value.
     * If found, it will use this container instead of starting a new one.
     * Otherwise, it starts a new container with the {@code quarkus-dev-service-google-cloud-spanner} label set to the
     * specified value.
     */
    @WithDefault("spanner")
    String serviceName();

}
