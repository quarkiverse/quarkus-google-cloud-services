package io.quarkiverse.googlecloudservices.logging.runtime;

import java.util.Map;
import java.util.Optional;

import com.google.cloud.logging.Severity;
import com.google.cloud.logging.Synchronicity;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "google.cloud.logging", phase = ConfigPhase.RUN_TIME)
public class LoggingConfiguration {

    /**
     * Which Google Operations log should be used by default. This value can be overridden
     * in code per record basis.
     */
    @ConfigItem
    public String defaultLog;

    /**
     * Enable or disable the Goocle Cloud logging.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * Configure log record MDC handling.
     */
    @ConfigItem
    public MDCConfig mdc;

    /**
     * Configure log record parameter handling.
     */
    @ConfigItem
    public ParametersConfig parameters;

    /**
     * Configure GCP logging synchronicity.
     */
    @ConfigItem
    public Optional<Synchronicity> synchronicity;

    /**
     * Configure auto flush level.
     */
    @ConfigItem
    public Optional<ConfigLevel> flushLevel;

    /**
     * Configure default labels.
     */
    @ConfigItem
    public Map<String, String> defaultLabel;

    /**
     * Configure log record stackl trace handling.
     */
    @ConfigItem
    public StackTraceConfig stackTrace;

    /**
     * Configured the monitored resource. Please consult the Google
     * documentation for the correct values: https://cloud.google.com/logging/docs/api/v2/resource-list#resource-types
     */
    @ConfigItem
    public ResourceConfig resource;

    @ConfigGroup
    public static class ResourceConfig {

        /**
         * The resource type of the log.
         */
        @ConfigItem(defaultValue = "global")
        public String type;

        /**
         * Resource labels.
         */
        @ConfigItem
        public Map<String, String> label;

    }

    @ConfigGroup
    public static class MDCConfig {

        /**
         * Include MDC values in the log.
         */
        @ConfigItem(defaultValue = "true")
        public boolean included;

        /**
         * Field name for MDC values, defaults to 'mdc'.
         */
        @ConfigItem(defaultValue = "mdc")
        public String fieldName;

    }

    @ConfigGroup
    public static class StackTraceConfig {

        /**
         * Include stack traces when exceptions are thrown.
         */
        @ConfigItem(defaultValue = "true")
        public boolean included;

        /**
         * Should stack traces be rendered as strings or arrays?
         */
        @ConfigItem(defaultValue = "STRING")
        public StackTraceRendering rendering;

    }

    @ConfigGroup
    public static class ParametersConfig {

        /**
         * Include parameter values in the log.
         */
        @ConfigItem(defaultValue = "true")
        public boolean included;

        /**
         * Field name for parameter values, defaults to 'parameters'.
         */
        @ConfigItem(defaultValue = "parameters")
        public String fieldName;

    }

    public enum ConfigLevel {
        DEBUG(Severity.DEBUG),
        INFO(Severity.INFO),
        WARN(Severity.WARNING),
        ERROR(Severity.ERROR),
        FATAL(Severity.CRITICAL);

        private Severity severity;

        private ConfigLevel(Severity severity) {
            this.severity = severity;
        }

        public Severity getSeverity() {
            return severity;
        }
    }

    public enum StackTraceRendering {
        STRING,
        ARRAY
    }
}