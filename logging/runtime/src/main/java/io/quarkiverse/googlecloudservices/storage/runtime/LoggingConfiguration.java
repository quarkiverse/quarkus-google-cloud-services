package io.quarkiverse.googlecloudservices.storage.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "google.cloud.logging", phase = ConfigPhase.RUN_TIME)
public class LoggingConfiguration {

    /**
     * Indicates that the log entries are structured, if for example Quarkus logging
     * (https://github.com/quarkiverse/quarkus-logging-json) is enabled.
     */
    @ConfigItem(defaultValue = "false")
    public boolean structured;

    /**
     * Which Google Operations log should be used.
     */
    @ConfigItem
    public String log;

    /**
     * Enable or disable the Goocle Cloud logging.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

}