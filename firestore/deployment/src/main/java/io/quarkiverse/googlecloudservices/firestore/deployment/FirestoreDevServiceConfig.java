package io.quarkiverse.googlecloudservices.firestore.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration group for the Firestore dev service. This class holds all the configuration properties
 * related to the Google Cloud Firestore service for development environments.
 * <p>
 * Here is an example of how to configure these properties:
 * <p>
 *
 * <pre>
 * quarkus.google.cloud.firestore.devservice.enabled = true
 * quarkus.google.cloud.firestore.devservice.image-name = gcr.io/google.com/cloudsdktool/google-cloud-cli # optional
 * quarkus.google.cloud.firestore.devservice.emulatorPort = 8080 # optional
 * </pre>
 */
@ConfigMapping(prefix = "quarkus.google.cloud.firestore.devservice")
@ConfigGroup
public interface FirestoreDevServiceConfig {

    /**
     * Indicates whether the Firestore service should be enabled or not.
     * The default value is 'false'.
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * Sets the Docker image name for the Google Cloud SDK.
     * This image is used to emulate the Firestore service in the development environment.
     * The default value is 'gcr.io/google.com/cloudsdktool/google-cloud-cli'.
     */
    @WithDefault("gcr.io/google.com/cloudsdktool/google-cloud-cli")
    String imageName();

    /**
     * Specifies the emulatorPort on which the Firestore service should run in the development environment.
     */
    Optional<Integer> emulatorPort();
}
