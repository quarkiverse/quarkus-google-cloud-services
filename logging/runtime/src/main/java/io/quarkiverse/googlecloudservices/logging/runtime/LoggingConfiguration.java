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
     * Configure base formatting to be either plain text or
     * structured json.
     */
    @ConfigItem(defaultValue = "TEXT")
    public LogFormat format;

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
     * Configure log record stack trace handling.
     */
    @ConfigItem
    public StackTraceConfig stackTrace;

    /**
     * Configured the monitored resource. Please consult the Google
     * documentation for the correct values: https://cloud.google.com/logging/docs/api/v2/resource-list#resource-types
     */
    @ConfigItem
    public ResourceConfig resource;

    /**
     * Configure how trace information is handled in GCP.
     */
    @ConfigItem
    public GcpTracingConfig gcpTracing;

    @ConfigGroup
    public static class GcpTracingConfig {

        /**
         * Use this setting to determine if extracted trace ID's should
         * also be forwarded to GCP for linking with GCP Operations Tracing.
         */
        @ConfigItem(defaultValue = "true")
        public boolean enabled;

        /**
         * If the GCP Operations Tracing is in another project, configure it
         * here. By default the logging project will be used.
         */
        @ConfigItem
        public Optional<String> projectId;
    }

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

        /**
         * If the stack is rendered as an array, what format should each
         * stack frame have?
         */
        @ConfigItem(defaultValue = "STRING")
        public StackElementRendering elementRendering;

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

    public enum StackElementRendering {
        STRING,
        OBJECT
    }

    public enum LogFormat {
        TEXT,
        JSON
    }
}