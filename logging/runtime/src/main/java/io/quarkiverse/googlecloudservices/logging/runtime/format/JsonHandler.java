package io.quarkiverse.googlecloudservices.logging.runtime.format;

import java.util.Map;
import java.util.logging.ErrorManager;

import org.jboss.logmanager.ExtLogRecord;

import com.google.cloud.logging.Payload;

import io.quarkiverse.googlecloudservices.logging.runtime.LoggingConfiguration;
import io.quarkiverse.googlecloudservices.logging.runtime.TraceInfo;
import io.quarkiverse.googlecloudservices.logging.runtime.ecs.EscJsonFormat;

public class JsonHandler implements InternalHandler {

    private final EscJsonFormat jsonFormat;

    public JsonHandler(LoggingConfiguration config, ErrorManager errorManager) {
        this.jsonFormat = createJsonFormatter(config, errorManager);
    }

    private EscJsonFormat createJsonFormatter(LoggingConfiguration config, ErrorManager errorManager) {
        EscJsonFormat form = new EscJsonFormat();
        form.init(config, errorManager);
        return form;
    }

    @Override
    public Payload<?> transform(ExtLogRecord record, TraceInfo trace) {
        Map<String, ?> json = jsonFormat.format(record, trace);
        return json == null ? null : Payload.JsonPayload.of(json);
    }
}
