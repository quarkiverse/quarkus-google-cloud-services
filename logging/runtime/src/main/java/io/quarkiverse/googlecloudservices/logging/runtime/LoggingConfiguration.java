package io.quarkiverse.googlecloudservices.logging.runtime;

import java.util.Map;
import java.util.Optional;

import com.google.cloud.logging.LoggingHandler.LogTarget;
import com.google.cloud.logging.Severity;
import com.google.cloud.logging.Synchronicity;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.google.cloud.logging")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface LoggingConfiguration {

    /**
     * Which Google Operations log should be used by default.
     */
    String defaultLog();

    /**
     * Enable or disable the Google Cloud logging.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Configure base formatting to be either plain text or
     * structured json. Allowed values: TEXT|JSON
     */
    @WithDefault("TEXT")
    LogFormat format();

    /**
     * Configure GCP logging synchronicity. Allowed values:
     * SYNC|ASYNC
     */
    Optional<Synchronicity> synchronicity();

    /**
     * Configure auto flush level. Allowed values:
     * DEBUG|INFO|WARN|ERROR|FATAL
     */
    Optional<ConfigLevel> flushLevel();

    /**
     * Configure default labels.
     */
    Map<String, String> defaultLabel();

    /**
     * Configured the monitored resource. Please consult the Google
     * documentation for the correct values.
     *
     * @see https://cloud.google.com/logging/docs/api/v2/resource-list#resource-types
     */
    ResourceConfig resource();

    /**
     * Configure how trace information is handled in GCP.
     */
    GcpTracingConfig gcpTracing();

    /**
     * Configuration options for structured logging.
     */
    StructuredConfig structured();

    /**
     * Configures if logs should be written to stdout or stderr instead of using Google Cloud Operations API.
     * Useful if app is deployed to managed Google Cloud Platform environment with installed logger agent.
     * Possible values: STDOUT, STDERR and CLOUD_LOGGING.
     */
    @WithDefault("CLOUD_LOGGING")
    LogTarget logTarget();

    interface StructuredConfig {

        /**
         * Configure log record stack trace handling.
         */
        StackTraceConfig stackTrace();

        /**
         * Configure log record MDC handling.
         */
        MDCConfig mdc();

        /**
         * Configure log record parameter handling.
         */
        ParametersConfig parameters();
    }

    interface GcpTracingConfig {

        /**
         * Use this setting to determine if extracted trace ID's should
         * also be forwarded to GCP for linking with GCP Operations Tracing.
         */
        @WithDefault("true")
        boolean enabled();

        /**
         * If the GCP Operations Tracing is in another project, configure it
         * here. By default the logging project will be used.
         */
        Optional<String> projectId();
    }

    interface ResourceConfig {

        /**
         * The resource type of the log.
         */
        @WithDefault("global")
        String type();

        /**
         * Resource labels.
         */
        Map<String, String> label();

    }

    interface MDCConfig {

        /**
         * Include MDC values in the log.
         */
        @WithDefault("true")
        boolean included();

        /**
         * Field name for MDC values, defaults to 'mdc'.
         */
        @WithDefault("mdc")
        String fieldName();

    }

    interface StackTraceConfig {

        /**
         * Include stack traces when exceptions are thrown.
         */
        @WithDefault("true")
        boolean included();

    }

    interface ParametersConfig {

        /**
         * Include parameter values in the log.
         */
        @WithDefault("true")
        boolean included();

        /**
         * Field name for parameter values, defaults to 'parameters'.
         */
        @WithDefault("parameters")
        String fieldName();

    }

    enum ConfigLevel {
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

    enum LogFormat {
        TEXT,
        JSON
    }

}