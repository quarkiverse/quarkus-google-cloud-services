package io.quarkiverse.googlecloudservices.bigtable.deployment;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * Root configuration class for Bigtable that operates at build time.
 * This class provides a nested structure for configuration, including
 * a separate group for the development service configuration.
 */
@ConfigMapping(prefix = "quarkus.google.cloud.bigtable")
@ConfigRoot
public interface BigtableBuildTimeConfig {

    /**
     * Configuration for the Bigtable dev service.
     * These settings will be used when Bigtable service is being configured
     * for development purposes.
     */
    BigtableDevServiceConfig devservice();
}
