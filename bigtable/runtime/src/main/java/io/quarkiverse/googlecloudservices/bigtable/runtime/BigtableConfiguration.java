package io.quarkiverse.googlecloudservices.bigtable.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Root configuration class for Bigtable that operates at runtime.
 * This class provides a nested structure for configuration, including
 * a separate group for the client configuration.
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class BigtableConfiguration {

    /**
     * Configuration for the Bigtable clients.
     * These settings will be used when Bigtable clients are being configured.
     */
    @ConfigItem
    public Map<String, BigTableClientConfiguration> clients;
}
