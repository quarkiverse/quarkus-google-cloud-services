package io.quarkiverse.googlecloudservices.firestore.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Configuration group for the Firestore dev service. This class holds all the configuration properties
 * related to the Google Cloud Firestore service for development environments.
 * <p>
 * Here is an example of how to configure these properties:
 * <p>
 *
 * <pre>
 * quarkus.firestore-dev-service.enabled = true
 * quarkus.firestore-dev-service.image-name = gcr.io/google.com/cloudsdktool/google-cloud-cli # optional
 * quarkus.firestore-dev-service.emulatorPort = 8080 # optional
 * </pre>
 */
@ConfigGroup
public class FirestoreDevServiceConfig {

    /**
     * Indicates whether the Firestore service should be enabled or not.
     * The default value is 'false'.
     */
    @ConfigItem(defaultValue = "false")
    public boolean enabled;

    /**
     * Sets the Docker image name for the Google Cloud SDK.
     * This image is used to emulate the Firestore service in the development environment.
     * The default value is 'gcr.io/google.com/cloudsdktool/google-cloud-cli'.
     */
    @ConfigItem(defaultValue = "gcr.io/google.com/cloudsdktool/google-cloud-cli")
    public String imageName;

    /**
     * Specifies the emulatorPort on which the Firestore service should run in the development environment.
     */
    @ConfigItem
    public Optional<Integer> emulatorPort = Optional.empty();
}
