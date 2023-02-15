package io.quarkiverse.googlecloudservices.logging.runtime.format;

import org.jboss.logmanager.ExtLogRecord;

import com.google.cloud.logging.Payload;

import io.quarkiverse.googlecloudservices.logging.runtime.TraceInfo;

public interface InternalHandler {

    public Payload<?> transform(ExtLogRecord record, TraceInfo trace);

}
