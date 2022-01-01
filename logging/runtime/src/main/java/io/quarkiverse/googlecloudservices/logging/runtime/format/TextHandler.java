package io.quarkiverse.googlecloudservices.logging.runtime.format;

import org.jboss.logmanager.ExtLogRecord;

import com.google.cloud.logging.Payload;

import io.quarkiverse.googlecloudservices.logging.runtime.TraceInfo;
import io.quarkiverse.googlecloudservices.logging.runtime.util.SimpleFormatter;

public class TextHandler implements InternalHandler {

    private static final SimpleFormatter MSG_FORMAT = new SimpleFormatter();

    @Override
    public Payload<?> transform(ExtLogRecord record, TraceInfo trace) {
        return Payload.StringPayload.of(MSG_FORMAT.format(record));
    }
}
