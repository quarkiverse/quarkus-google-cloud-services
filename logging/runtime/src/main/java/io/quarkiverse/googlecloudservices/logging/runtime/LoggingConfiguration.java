package io.quarkiverse.googlecloudservices.logging.runtime;

import java.util.Map;
import java.util.Optional;

import com.google.cloud.logging.LoggingHandler.LogTarget;
import com.google.cloud.logging.Severity;
import com.google.cloud.logging.Synchronicity;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "google.cloud.logging", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class LoggingConfiguration {

    /**
     * Which Google Operations log should be used by default.
     */
    @ConfigItem
    public String defaultLog;

    /**
     * Enable or disable the Google Cloud logging.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * Enable or disable default Quarkus console logging.
     */
    @ConfigItem
    public boolean enableConsoleLogging;

    /**
     * Configure base formatting to be either plain text or
     * structured json. Allowed values: TEXT|JSON
     */
    @ConfigItem(defaultValue = "TEXT")
    public LogFormat format;

    /**
     * Configure GCP logging synchronicity. Allowed values:
     * SYNC|ASYNC
     */
    @ConfigItem
    public Optional<Synchronicity> synchronicity;

    /**
     * Configure auto flush level. Allowed values:
     * DEBUG|INFO|WARN|ERROR|FATAL
     */
    @ConfigItem
    public Optional<ConfigLevel> flushLevel;

    /**
     * Configure default labels.
     */
    @ConfigItem
    public Map<String, String> defaultLabel;

    /**
     * Configured the monitored resource. Please consult the Google
     * documentation for the correct values.
     *
     * @see https://cloud.google.com/logging/docs/api/v2/resource-list#resource-types
     */
    @ConfigItem
    public ResourceConfig resource;

    /**
     * Configure how trace information is handled in GCP.
     */
    @ConfigItem
    public GcpTracingConfig gcpTracing;

    /**
     * Configuration options for structured logging.
     */
    @ConfigItem
    public StructuredConfig structured;

    /**
     * Configures if logs should be written to stdout or stderr instead of using Google Cloud Operations API.
     * Useful if app is deployed to managed Google Cloud Platform environment with installed logger agent.
     * Possible values: STDOUT, STDERR and CLOUD_LOGGING.
     */
    @ConfigItem(defaultValue = "CLOUD_LOGGING")
    public LogTarget logTarget;

    @ConfigGroup
    public static class StructuredConfig {

        /**
         * Configure log record stack trace handling.
         */
        @ConfigItem
        public StackTraceConfig stackTrace;

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
    }

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

    public enum LogFormat {
        TEXT,
        JSON
    }

}