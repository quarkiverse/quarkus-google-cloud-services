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

}
