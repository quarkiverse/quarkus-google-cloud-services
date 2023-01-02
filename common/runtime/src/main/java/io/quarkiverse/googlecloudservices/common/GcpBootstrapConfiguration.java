package io.quarkiverse.googlecloudservices.common;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Bootstrap configuration
 *
 * We need these properties at bootstrap to be able to register the secret manager config source that needs it.
 */
@ConfigRoot(name = "google.cloud", phase = ConfigPhase.BOOTSTRAP)
public class GcpBootstrapConfiguration {

    /**
     * Enable or disable metadata server access to retrieve configuration options (projectId, region...).
     */
    @ConfigItem()
    public boolean enableMetadataServer = true;

    /**
     * Google Cloud project ID.
     * It defaults to `ServiceOptions.getDefaultProjectId()` if `google.cloud.enable-metadata-server` is set to true (which is
     * the default),
     * so to the project ID corresponding to the default credentials if the default credentials are set, otherwise null.
     */
    @ConfigItem(defaultValueDocumentation = "ServiceOptions.getDefaultProjectId()")
    public Optional<String> projectId;

    /**
     * Google Cloud service account file location.
     */
    @ConfigItem
    public Optional<String> serviceAccountLocation;

    /**
     * Google Cloud service account base64 encoded content.
     */
    @ConfigItem
    public Optional<String> serviceAccountEncodedKey;

    /**
     * Enable Google Cloud access token authentication
     * For example, the access token which is returned as part of OpenId Connect Authorization Code Flow
     * may be used to access Google Cloud services on behalf of the authenticated user.
     *
     * Note that if a service account location is configured then the access token will be ignored even if this property is
     * enabled.
     *
     * Disable this property if the default Google Cloud authentication is required.
     */
    @ConfigItem(defaultValue = "true")
    public boolean accessTokenEnabled = true;
}
