package io.quarkiverse.googlecloudservices.storage.runtime;

import java.time.Instant;
import java.util.Map;
import java.util.logging.ErrorManager;
import java.util.logging.Level;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.MonitoredResource;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.Logging.WriteOption;
import com.google.cloud.logging.LoggingOptions;
import com.google.cloud.logging.Payload;
import com.google.cloud.logging.Severity;
import com.google.common.collect.ImmutableList;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;

import io.quarkiverse.googlecloudservices.common.GcpBootstrapConfiguration;
import io.quarkiverse.googlecloudservices.common.GcpConfigHolder;
import io.quarkiverse.googlecloudservices.storage.runtime.ecs.EscJsonFormatter;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;

// TODO: config write syncronozity
// TODO: config flush severity
// TODO: more labels
// TODO: custom formatters?
// TODO: configured monitored resource: https://cloud.google.com/monitoring/custom-metrics/creating-metrics#which-resource
// TODO: trace + span ID decoration
// TODO: config mdc name in esc
// TODO: config parameters name in esc
// TODO: config stack trace as array or string in esc
// TODO: additional parameters (pod name, host IP etc)

public class LoggingHandler extends ExtHandler {

    private final LoggingConfiguration config;
    private Logging log;
    private WriteOption[] defaultWriteOptions;
    private JsonFormatter jsonFormat;

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
            l.write(ImmutableList.of(logEntry), defaultWriteOptions);
        } catch (Exception ex) {
            getErrorManager().error(null, ex, ErrorManager.GENERIC_FAILURE);
        }
    }

    private LogEntry transform(ExtLogRecord record) {
        Map<String, ?> json = jsonFormat.format(record);
        Level level = record.getLevel();
        return LogEntry.newBuilder(Payload.JsonPayload.of(json))
                .setTimestamp(Instant.ofEpochMilli(record.getMillis()))
                .setSeverity(severityFor(level))
                .build();
    }

    private static Severity severityFor(Level level) {
        int i = level.intValue();
        if (i <= Level.FINE.intValue()) {
            return Severity.DEBUG;
        } else if (i <= Level.INFO.intValue()) {
            return Severity.INFO;
        } else if (i <= Level.WARNING.intValue()) {
            return Severity.WARNING;
        } else if (i <= Level.SEVERE.intValue()) {
            return Severity.ERROR;
        } else {
            return Severity.DEFAULT;
        }
    }

    @Override
    public void flush() {
        try {
            initGetLogging().flush();
        } catch (Exception ex) {
            getErrorManager().error(null, ex, ErrorManager.FLUSH_FAILURE);
        }
    }

    private synchronized Logging initGetLogging() {
        if (log == null) {
            InstanceHandle<GcpConfigHolder> config = Arc.container().instance(GcpConfigHolder.class);
            InstanceHandle<GoogleCredentials> creds = Arc.container().instance(GoogleCredentials.class);
            GcpBootstrapConfiguration gcpConfiguration = config.get().getBootstrapConfig();
            // create logger
            log = LoggingOptions.getDefaultInstance().toBuilder()
                    .setCredentials(creds.get())
                    .setProjectId(gcpConfiguration.projectId.orElse(null))
                    .build()
                    .getService();
            // create default write options
            defaultWriteOptions = new WriteOption[] {
                    WriteOption.logName(this.config.log),
                    WriteOption.resource(MonitoredResource.newBuilder("global").build())
            };
            // create json formatter
            InstanceHandle<JsonFormatter> jsonFormat = Arc.container().instance(JsonFormatter.class);
            if (jsonFormat.isAvailable()) {
                this.jsonFormat = jsonFormat.get();
            } else {
                this.jsonFormat = new EscJsonFormatter().toFormatter();
            }
        }
        return log;
    }
}
