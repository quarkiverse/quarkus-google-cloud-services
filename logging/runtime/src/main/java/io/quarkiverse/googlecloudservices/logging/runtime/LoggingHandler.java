package io.quarkiverse.googlecloudservices.logging.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.ErrorManager;

import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.Logging.WriteOption;
import com.google.cloud.logging.Payload;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;

import io.quarkiverse.googlecloudservices.logging.runtime.LoggingConfiguration.LogFormat;
import io.quarkiverse.googlecloudservices.logging.runtime.cdi.WriteOptionsHolder;
import io.quarkiverse.googlecloudservices.logging.runtime.format.InternalHandler;
import io.quarkiverse.googlecloudservices.logging.runtime.format.JsonHandler;
import io.quarkiverse.googlecloudservices.logging.runtime.format.TextHandler;
import io.quarkiverse.googlecloudservices.logging.runtime.util.LevelTransformer;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;

public class LoggingHandler extends ExtHandler {

    private final LoggingConfiguration config;

    // lazy values, they depend on the gcp config which in turn
    // depend on runtime configuration - not build time
    private Logging log;
    private String projectId;
    private WriteOption[] defaultWriteOptions;
    private InternalHandler internalHandler;
    private List<LabelExtractor> extractors;
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
            TraceInfo trace = traceExtractor.extract(record);
            LogEntry logEntry = transform(record, trace);
            if (logEntry != null) {
                l.write(ImmutableList.of(logEntry), defaultWriteOptions);
            }
        } catch (Exception ex) {
            getErrorManager().error("Failed to publish record to GCP", ex, ErrorManager.WRITE_FAILURE);
        }
    }

    private LogEntry transform(ExtLogRecord record, TraceInfo trace) {
        Payload<?> payload = internalHandler.transform(record, trace);
        if (payload != null) {
            Map<String, String> labels = extractLabels(record);
            com.google.cloud.logging.LogEntry.Builder builder = LogEntry.newBuilder(payload)
                    .setSeverity(LevelTransformer.toSeverity(record.getLevel()))
                    .setTimestamp(record.getInstant())
                    .setLabels(labels);
            if (this.config.gcpTracing.enabled && trace != null && !Strings.isNullOrEmpty(trace.getTraceId())) {
                builder = builder.setTrace(composeTraceString(trace.getTraceId()));
            }
            return builder.build();
        } else {
            return null;
        }
    }

    private String composeTraceString(String traceId) {
        return String.format("projects/%s/traces/%s", this.config.gcpTracing.projectId.orElse(projectId), traceId);
    }

    private Map<String, String> extractLabels(ExtLogRecord record) {
        if (this.extractors.isEmpty()) {
            return Collections.emptyMap();
        } else {
            Map<String, String> m = new HashMap<>(5);
            this.extractors.forEach(e -> {
                Map<String, String> extra = e.extract(record);
                if (extra != null) {
                    m.putAll(extra);
                }
            });
            return m;
        }
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
            initLogger();
            // create default write options
            initDefaultWriteOptions();
            // create json formatter
            initInternalHandler();
            // init label extractors
            initLabelExtractors();
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

    private void initLabelExtractors() {
        this.extractors = new ArrayList<>();
        InjectableInstance<LabelExtractor> handle = Arc.container().select(LabelExtractor.class);
        handle.forEach(this.extractors::add);
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
        this.defaultWriteOptions = Arc.container().instance(WriteOptionsHolder.class).get().getOptions();
    }

    private void initLogger() {
        this.log = Arc.container().instance(Logging.class).get();
    }
}
