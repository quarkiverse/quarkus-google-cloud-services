package io.quarkiverse.googlecloudservices.logging.runtime;

import java.util.Collections;
import java.util.logging.ErrorManager;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;

import com.google.cloud.MonitoredResource;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.Logging.WriteOption;
import com.google.cloud.logging.Payload;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import io.quarkiverse.googlecloudservices.logging.runtime.LoggingConfiguration.LogFormat;
import io.quarkiverse.googlecloudservices.logging.runtime.format.InternalHandler;
import io.quarkiverse.googlecloudservices.logging.runtime.format.JsonHandler;
import io.quarkiverse.googlecloudservices.logging.runtime.format.TextHandler;
import io.quarkiverse.googlecloudservices.logging.runtime.util.LevelTransformer;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;

public class LoggingHandler extends ExtHandler {

    private final LoggingConfiguration config;

    // lazy values, they depend on the gcp config which in turn
    // depend on runtime configuration - not build time
    private volatile Logging log;
    private WriteOption[] defaultWriteOptions;
    private InternalHandler internalHandler;
    private TraceInfoExtractor traceExtractor;

    public LoggingHandler(LoggingConfiguration config) {
        this.config = config;
    }

    @Override
    public void close() throws SecurityException {
        /**
         * Not implemented by choice: when quarkus shuts down the logger get closed
         * BEFORE the last log entries are written, and this causes problems when reconnecting
         * to Google Operations. We're relying on JVM shutdown here.
         */
    }

    @Override
    public void doPublish(ExtLogRecord record) {
        try {
            Logging l = initGetLogging();
            if (l == null) {
                // When this happens it's because the injection context has
                // been shut down - so we'll short-cut here
                return;
            }
            TraceInfo trace = traceExtractor.extract(record);
            LogEntry logEntry = transform(record, trace);
            if (logEntry != null) {
                switch (config.logTarget) {
                    case STDOUT:
                        System.out.println(logEntry.toStructuredJsonString());
                        break;
                    case STDERR:
                        System.err.println(logEntry.toStructuredJsonString());
                        break;
                    case CLOUD_LOGGING:
                        l.write(ImmutableList.of(logEntry), defaultWriteOptions);
                        break;
                }
            }
        } catch (Exception ex) {
            getErrorManager().error("Failed to write logs", ex, ErrorManager.WRITE_FAILURE);
        }
    }

    private LogEntry transform(ExtLogRecord record, TraceInfo trace) {
        Payload<?> payload = internalHandler.transform(record, trace);
        if (payload != null) {
            com.google.cloud.logging.LogEntry.Builder builder = LogEntry.newBuilder(payload)
                    .setSeverity(LevelTransformer.toSeverity(record.getLevel()))
                    .setTimestamp(record.getInstant());
            if (this.config.gcpTracing.enabled && trace != null && !Strings.isNullOrEmpty(trace.getTraceId())) {
                builder = builder.setTrace(composeTraceString(trace.getTraceId()));
            }
            return builder.build();
        } else {
            return null;
        }
    }

    private String composeTraceString(String traceId) {
        return String.format("projects/%s/traces/%s", this.config.gcpTracing.projectId.orElse(null), traceId);
    }

    @Override
    public void flush() {
        try {
            initGetLogging().flush();
        } catch (Exception ex) {
            getErrorManager().error("Failed to fluch GCP logger", ex, ErrorManager.FLUSH_FAILURE);
        }
    }

    private synchronized Logging initGetLogging() {
        if (log == null) {
            // create logger
            if (!initLogger()) {
                return null;
            }
            // create default write options
            initDefaultWriteOptions();
            // create json formatter
            initInternalHandler();
            // init trace extractor
            initTraceExtractor();
        }
        return log;
    }

    private void initInternalHandler() {
        if (this.config.format == LogFormat.JSON) {
            this.internalHandler = new JsonHandler(this.config, getErrorManager());
        } else {
            this.internalHandler = new TextHandler();
        }
    }

    private void initTraceExtractor() {
        InstanceHandle<TraceInfoExtractor> handle = Arc.container().instance(TraceInfoExtractor.class);
        if (handle.isAvailable()) {
            this.traceExtractor = handle.get();
        } else {
            this.traceExtractor = (r) -> null;
        }
    }

    private void initDefaultWriteOptions() {
        this.defaultWriteOptions = new WriteOption[] {
                WriteOption.logName(this.config.defaultLog),
                WriteOption.resource(createMonitoredResource()),
                WriteOption.labels(this.config.defaultLabel == null ? Collections.emptyMap() : this.config.defaultLabel)
        };
    }

    private MonitoredResource createMonitoredResource() {
        MonitoredResource.Builder b = MonitoredResource.newBuilder(this.config.resource.type);
        if (this.config.resource.label != null) {
            this.config.resource.label.forEach(b::addLabel);
        }
        return b.build();
    }

    // return false if the CDI is shut down
    private boolean initLogger() {
        ArcContainer container = Arc.container();
        if (container != null) {
            this.log = container.instance(Logging.class).get();
            return true;
        } else {
            return false;
        }
    }
}
