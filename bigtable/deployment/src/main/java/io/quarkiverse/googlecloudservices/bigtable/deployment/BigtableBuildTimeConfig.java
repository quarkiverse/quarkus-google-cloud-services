package io.quarkiverse.googlecloudservices.bigtable.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Root configuration class for Bigtable that operates at build time.
 * This class provides a nested structure for configuration, including
 * a separate group for the development service configuration.
 */
@ConfigRoot(name = "google.cloud.bigtable", phase = ConfigPhase.BUILD_TIME)
public class BigtableBuildTimeConfig {

    /**
     * Configuration for the Bigtable dev service.
     * These settings will be used when Bigtable service is being configured
     * for development purposes.
     */
    @ConfigItem
    public BigtableDevServiceConfig devservice;
}
