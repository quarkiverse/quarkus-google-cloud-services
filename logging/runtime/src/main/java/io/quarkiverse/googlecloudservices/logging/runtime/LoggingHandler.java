package io.quarkiverse.googlecloudservices.logging.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.ErrorManager;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.MonitoredResource;
import com.google.cloud.MonitoredResource.Builder;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.Logging.WriteOption;
import com.google.cloud.logging.LoggingOptions;
import com.google.cloud.logging.Payload;
import com.google.common.collect.ImmutableList;

import io.quarkiverse.googlecloudservices.common.GcpBootstrapConfiguration;
import io.quarkiverse.googlecloudservices.common.GcpConfigHolder;
import io.quarkiverse.googlecloudservices.logging.runtime.ecs.EscJsonFormat;
import io.quarkiverse.googlecloudservices.logging.runtime.util.LevelTransformer;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;

// TODO: trace + span ID decoration
// TODO: config stack trace as array or string in esc
// TODO: additional labels per record

public class LoggingHandler extends ExtHandler {

    private final LoggingConfiguration config;

    // lazy values, the depend on the gcp config (which in turn)
    // depend on runtime configuration, not build time
    private Logging log;
    private WriteOption[] defaultWriteOptions;
    private JsonFormatter jsonFormat;
    private List<LabelExtractor> extractors;

    public LoggingHandler(LoggingConfiguration config) {
        this.config = config;
    }

    @Override
    public synchronized void close() throws SecurityException {
        if (log != null) {
            try {
                log.close();
            } catch (Exception ex) {
                // ignore
            } finally {
                log = null;
            }
        }
    }

    @Override
    public void doPublish(ExtLogRecord record) {
        try {
            Logging l = initGetLogging();
            LogEntry logEntry = transform(record);
            if (logEntry != null) {
                l.write(ImmutableList.of(logEntry), defaultWriteOptions);
            }
        } catch (Exception ex) {
            getErrorManager().error("Failed to publish record to GCP", ex, ErrorManager.WRITE_FAILURE);
        }
    }

    private LogEntry transform(ExtLogRecord record) {
        Map<String, ?> json = jsonFormat.format(record);
        if (json != null) {
            Map<String, String> labels = extractLabels(record);
            return LogEntry.newBuilder(Payload.JsonPayload.of(json))
                    .setSeverity(LevelTransformer.toSeverity(record.getLevel()))
                    .setTimestamp(record.getInstant())
                    .setLabels(labels)
                    .build();
        } else {
            return null;
        }
    }

    private Map<String, String> extractLabels(ExtLogRecord record) {
        if (this.extractors.isEmpty()) {
            return Collections.emptyMap();
        } else {
            Map<String, String> m = new HashMap<>(5);
            this.extractors.forEach(e -> m.putAll(e.extract(record)));
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
            // get hold of the GCP config
            InstanceHandle<GcpConfigHolder> config = Arc.container().instance(GcpConfigHolder.class);
            // create logger
            initLogger(config);
            // create default write options
            initDefaultWriteOptions();
            // check auto-flush and synchronizity 
            this.config.flushLevel.ifPresent(level -> log.setFlushSeverity(level.getSeverity()));
            this.config.synchronicity.ifPresent(sync -> log.setWriteSynchronicity(sync));
            // create json formatter
            initJsonFormatter();
            // init label extractors
            initLabelExtractors();
        }
        return log;
    }

    private void initLabelExtractors() {
        this.extractors = new ArrayList<>();
        InjectableInstance<LabelExtractor> handle = Arc.container().select(LabelExtractor.class);
        handle.forEach(this.extractors::add);
    }

    private void initJsonFormatter() {
        InstanceHandle<JsonFormatter> jsonFormat = Arc.container().instance(JsonFormatter.class);
        if (jsonFormat.isAvailable()) {
            this.jsonFormat = jsonFormat.get();
        } else {
            this.jsonFormat = EscJsonFormat.createFormatter();
        }
        // config formatter
        this.jsonFormat.init(this.config, getErrorManager());
    }

    private void initDefaultWriteOptions() {
        defaultWriteOptions = new WriteOption[] {
                WriteOption.logName(this.config.defaultLog),
                WriteOption.resource(createMonitoredResource()),
                WriteOption.labels(this.config.defaultLabel == null ? Collections.emptyMap() : this.config.defaultLabel)
        };
    }

    private void initLogger(InstanceHandle<GcpConfigHolder> config) {
        InstanceHandle<GoogleCredentials> creds = Arc.container().instance(GoogleCredentials.class);
        GcpBootstrapConfiguration gcpConfiguration = config.get().getBootstrapConfig();
        log = LoggingOptions.getDefaultInstance().toBuilder()
                .setCredentials(creds.get())
                .setProjectId(gcpConfiguration.projectId.orElse(null))
                .build()
                .getService();
    }

    private MonitoredResource createMonitoredResource() {
        Builder b = MonitoredResource.newBuilder(this.config.resource.type);
        if (this.config.resource.label != null) {
            this.config.resource.label.forEach((k, v) -> b.addLabel(k, v));
        }
        return b.build();
    }
}
