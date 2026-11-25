package io.quarkiverse.googlecloudservices.secretmanager.runtime;

import java.util.Optional;

import io.quarkiverse.googlecloudservices.common.GcpBootstrapConfiguration;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * Secret Manager configuration.
 */
@ConfigMapping(prefix = "quarkus.google.cloud.secret-manager")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface SecretManagerConfiguration {

    /**
     * Google Cloud service account file location for the Secret Manager service.
     * <p>
     * When either this property or `service-account-encoded-key` is set, the global service account configuration is
     * ignored for Secret Manager. Otherwise, the value of `quarkus.google.cloud.service-account-location` is used.
     */
    Optional<String> serviceAccountLocation();

    /**
     * Google Cloud service account base64 encoded content for the Secret Manager service.
     * <p>
     * When either this property or `service-account-location` is set, the global service account configuration is
     * ignored for Secret Manager. Otherwise, the value of `quarkus.google.cloud.service-account-encoded-key` is used.
     */
    Optional<String> serviceAccountEncodedKey();

    /**
     * Enable Google Cloud access token authentication for the Secret Manager service.
     * <p>
     * When this property is not set, the value of `quarkus.google.cloud.access-token-enabled` is used.
     * <p>
     * Disable this property when the Secret Manager client must use the configured service account or the
     * application default credentials, even if access token authentication is enabled globally.
     */
    Optional<Boolean> accessTokenEnabled();

    /**
     * Returns whether a service account is configured specifically for Secret Manager.
     */
    default boolean hasServiceAccountConfiguration() {
        return serviceAccountLocation().isPresent() || serviceAccountEncodedKey().isPresent();
    }

    /**
     * Returns the service account file location to use for Secret Manager, falling back to the global configuration
     * when no service account is configured for this service.
     */
    default Optional<String> effectiveServiceAccountLocation(GcpBootstrapConfiguration globalConfiguration) {
        return hasServiceAccountConfiguration() ? serviceAccountLocation() : globalConfiguration.serviceAccountLocation();
    }

    /**
     * Returns the service account base64 encoded content to use for Secret Manager, falling back to the global
     * configuration when no service account is configured for this service.
     */
    default Optional<String> effectiveServiceAccountEncodedKey(GcpBootstrapConfiguration globalConfiguration) {
        return hasServiceAccountConfiguration() ? serviceAccountEncodedKey()
                : globalConfiguration.serviceAccountEncodedKey();
    }

    /**
     * Returns whether access token authentication is enabled for Secret Manager, falling back to the global
     * configuration when the property is not set.
     */
    default boolean effectiveAccessTokenEnabled(GcpBootstrapConfiguration globalConfiguration) {
        return accessTokenEnabled().orElseGet(globalConfiguration::accessTokenEnabled);
    }
}
