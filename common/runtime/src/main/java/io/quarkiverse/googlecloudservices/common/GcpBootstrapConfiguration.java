package io.quarkiverse.googlecloudservices.common;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Bootstrap configuration
 *
 * We need these properties at bootstrap to be able to register the secret manager config source that needs it.
 */
@ConfigMapping(prefix = "quarkus.google.cloud")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface GcpBootstrapConfiguration {

    /**
     * Enable or disable metadata server access to retrieve configuration options (projectId, region...).
     */
    @WithDefault("true")
    boolean enableMetadataServer();

    /**
     * Google Cloud project ID.
     * It defaults to `ServiceOptions.getDefaultProjectId()` if `google.cloud.enable-metadata-server` is set to true (which is
     * the default),
     * so to the project ID corresponding to the default credentials if the default credentials are set, otherwise null.
     */
    Optional<String> projectId();

    /**
     * Google Cloud service account file location.
     */
    Optional<String> serviceAccountLocation();

    /**
     * Google Cloud service account base64 encoded content.
     */
    Optional<String> serviceAccountEncodedKey();

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
    @WithDefault("true")
    boolean accessTokenEnabled();

    /**
     * Whether to enable the secret manager
     */
    @WithDefault("true")
    boolean secretManagerEnabled();
}
