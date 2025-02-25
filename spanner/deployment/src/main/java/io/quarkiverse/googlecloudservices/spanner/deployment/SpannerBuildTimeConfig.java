package io.quarkiverse.googlecloudservices.spanner.deployment;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * Root configuration class for Google Cloud Spanner that operates at build time.
 * This class provides a nested structure for configuration, including
 * a separate group for the development service configuration.
 */
@ConfigMapping(prefix = "quarkus.google.cloud.spanner")
@ConfigRoot
public interface SpannerBuildTimeConfig {

    /**
     * Configuration for the Spanner dev service.
     * These settings will be used when Spanner service is being configured
     * for development purposes.
     */
    SpannerDevServiceConfig devservice();

}
