package io.quarkiverse.googlecloudservices.logging.runtime.format;

import java.util.Map;
import java.util.logging.ErrorManager;

import org.jboss.logmanager.ExtLogRecord;

import com.google.cloud.logging.Payload;

import io.quarkiverse.googlecloudservices.logging.runtime.JsonFormatter;
import io.quarkiverse.googlecloudservices.logging.runtime.LoggingConfiguration;
import io.quarkiverse.googlecloudservices.logging.runtime.TraceInfo;
import io.quarkiverse.googlecloudservices.logging.runtime.ecs.EscJsonFormat;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;

public class JsonHandler implements InternalHandler {

    private final JsonFormatter jsonFormat;

    public JsonHandler(LoggingConfiguration config, ErrorManager errorManager) {
        this.jsonFormat = createJsonFormatter(config, errorManager);
    }

    private JsonFormatter createJsonFormatter(LoggingConfiguration config, ErrorManager errorManager) {
        JsonFormatter form = null;
        InstanceHandle<JsonFormatter> jsonFormat = Arc.container().instance(JsonFormatter.class);
        if (jsonFormat.isAvailable()) {
            form = jsonFormat.get();
        } else {
            form = EscJsonFormat.createFormatter();
        }
        // config formatter
        form.init(config, errorManager);
        return form;
    }

    @Override
    public Payload<?> transform(ExtLogRecord record, TraceInfo trace) {
        Map<String, ?> json = jsonFormat.format(record, trace);
        return Payload.JsonPayload.of(json);
    }
}
