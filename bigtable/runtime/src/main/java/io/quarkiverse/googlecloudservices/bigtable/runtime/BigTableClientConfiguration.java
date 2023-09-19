package io.quarkiverse.googlecloudservices.bigtable.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Configuration for the Bigtable clients.
 * These settings will be used when Bigtable clients are being configured.
 */
@ConfigGroup
public class BigTableClientConfiguration {

    /**
     * The project id.
     */
    @ConfigItem
    public String instanceId;
}
