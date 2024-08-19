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
 * quarkus.google.cloud.bigtable.devservice.image-name = gcr.io/google.com/cloudsdktool/google-cloud-cli # optional
 * quarkus.google.cloud.bigtable.devservice.emulatorPort = 9000 # optional
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
     * The default value is 'gcr.io/google.com/cloudsdktool/google-cloud-cli'.
     */
    @WithDefault("gcr.io/google.com/cloudsdktool/google-cloud-cli")
    String imageName();

    /**
     * Specifies the emulatorPort on which the Bigtable service should run in the development environment.
     */
    Optional<Integer> emulatorPort();
}
