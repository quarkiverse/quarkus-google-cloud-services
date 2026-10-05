package io.quarkiverse.googlecloudservices.bigtable.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration group for the Bigtable dev service. This class holds all the configuration properties
 * related to the Google Cloud Bigtable service for development environments.
 * <p>
 * Here is an example of how to configure these properties:
 * <p>
 *
 * <pre>
 * quarkus.google.cloud.bigtable.devservice.enabled = true
 * quarkus.google.cloud.bigtable.devservice.image-name = gcr.io/google.com/cloudsdktool/google-cloud-cli:emulators # optional
 * quarkus.google.cloud.bigtable.devservice.emulatorPort = 9000 # optional
 * quarkus.google.cloud.bigtable.devservice.shared = true # optional
 * quarkus.google.cloud.bigtable.devservice.service-name = bigtable # optional
 * </pre>
 */
@ConfigMapping(prefix = "quarkus.google.cloud.bigtable.devservice")
@ConfigGroup
public interface BigtableDevServiceConfig {

    /**
     * Indicates whether the Bigtable service should be enabled or not.
     * The default value is 'false'.
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * Sets the Docker image name for the Google Cloud SDK.
     * This image is used to emulate the Bigtable service in the development environment.
     * The default value is 'gcr.io/google.com/cloudsdktool/google-cloud-cli:emulators'.
     */
    @WithDefault("gcr.io/google.com/cloudsdktool/google-cloud-cli:emulators")
    String imageName();

    /**
     * Specifies the emulatorPort on which the Bigtable service should run in the development environment.
     */
    Optional<Integer> emulatorPort();

    /**
     * Indicates if the Bigtable emulator managed by Dev Services is shared.
     * When shared, Quarkus looks for running containers using label-based service discovery.
     * If a matching container is found, it is used, and so a second one is not started.
     * Otherwise, Dev Services starts a new container.
     * <p>
     * The discovery uses the {@code quarkus-dev-service-google-cloud-bigtable} label.
     * The value is configured using the {@code service-name} property.
     * <p>
     * Container sharing is only used in dev mode.
     */
    @WithDefault("true")
    boolean shared();

    /**
     * The value of the {@code quarkus-dev-service-google-cloud-bigtable} label attached to the started container.
     * This property is used when {@code shared} is set to {@code true}.
     * In this case, before starting a container, Dev Services looks for a container with the
     * {@code quarkus-dev-service-google-cloud-bigtable} label set to the configured value.
     * If found, it will use this container instead of starting a new one.
     * Otherwise, it starts a new container with the {@code quarkus-dev-service-google-cloud-bigtable} label set to the
     * specified value.
     */
    @WithDefault("bigtable")
    String serviceName();
}
