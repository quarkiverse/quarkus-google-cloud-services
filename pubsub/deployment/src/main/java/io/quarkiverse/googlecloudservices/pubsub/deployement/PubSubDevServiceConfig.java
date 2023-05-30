package io.quarkiverse.googlecloudservices.pubsub.deployement;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Configuration group for the PubSubDevService. This class holds all the configuration properties
 * related to the Google Cloud Pub/Sub service for development environments.
 * <p>
 * Here is an example of how to configure these properties:
 * <p>
 *
 * <pre>
 * quarkus.pub-sub-dev-service.enabled = true
 * quarkus.pub-sub-dev-service.image-name = gcr.io/google.com/cloudsdktool/google-cloud-cli:380.0.0-emulators
 * quarkus.pub-sub-dev-service.emulatorPort = 8085
 * </pre>
 */
@ConfigGroup
public class PubSubDevServiceConfig {

    /**
     * Indicates whether the Pub/Sub service should be enabled or not.
     * The default value is 'true'.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * Sets the Docker image name for the Google Cloud SDK.
     * This image is used to emulate the Pub/Sub service in the development environment.
     * The default value is 'gcr.io/google.com/cloudsdktool/google-cloud-cli:380.0.0-emulators'.
     */
    @ConfigItem(name = "image-name", defaultValue = "gcr.io/google.com/cloudsdktool/google-cloud-cli:380.0.0-emulators")
    public String imageName;

    /**
     * Specifies the emulatorPort on which the Pub/Sub service should run in the development environment.
     * The default value is '8085'.
     */
    @ConfigItem(name = "emulatorPort", defaultValue = "8085")
    public int port;
}
