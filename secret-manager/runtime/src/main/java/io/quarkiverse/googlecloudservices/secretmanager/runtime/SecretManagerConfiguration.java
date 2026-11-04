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
     * Enable Google Cloud access token authentication for the Secret Manager service.
     * <p>
     * When this property is not set, the value of `quarkus.google.cloud.access-token-enabled` is used.
     * <p>
     * Disable this property when the Secret Manager client must use the configured service account or the
     * application default credentials, even if access token authentication is enabled globally.
     */
    Optional<Boolean> accessTokenEnabled();

    /**
     * Returns whether access token authentication is enabled for Secret Manager, falling back to the global
     * configuration when the property is not set.
     */
    default boolean effectiveAccessTokenEnabled(GcpBootstrapConfiguration globalConfiguration) {
        return accessTokenEnabled().orElseGet(globalConfiguration::accessTokenEnabled);
    }
}
