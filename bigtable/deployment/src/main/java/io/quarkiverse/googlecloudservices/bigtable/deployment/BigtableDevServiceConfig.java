package io.quarkiverse.googlecloudservices.bigtable.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Configuration group for the Bigtable dev service. This class holds all the configuration properties
 * related to the Google Cloud Bigtable service for development environments.
 * <p>
 * Here is an example of how to configure these properties:
 * <p>
 *
 * <pre>
 * quarkus.google.cloud.bigtable.deservice.enabled = true
 * quarkus.google.cloud.bigtable.deservice.image-name = gcr.io/google.com/cloudsdktool/google-cloud-cli # optional
 * quarkus.google.cloud.bigtable.deservice.emulatorPort = 9000 # optional
 * </pre>
 */
@ConfigGroup
public class BigtableDevServiceConfig {

    /**
     * Indicates whether the Bigtable service should be enabled or not.
     * The default value is 'false'.
     */
    @ConfigItem(defaultValue = "false")
    public boolean enabled;

    /**
     * Sets the Docker image name for the Google Cloud SDK.
     * This image is used to emulate the Bigtable service in the development environment.
     * The default value is 'gcr.io/google.com/cloudsdktool/google-cloud-cli'.
     */
    @ConfigItem(defaultValue = "gcr.io/google.com/cloudsdktool/google-cloud-cli")
    public String imageName;

    /**
     * Specifies the emulatorPort on which the Bigtable service should run in the development environment.
     */
    @ConfigItem
    public Optional<Integer> emulatorPort = Optional.empty();
}
