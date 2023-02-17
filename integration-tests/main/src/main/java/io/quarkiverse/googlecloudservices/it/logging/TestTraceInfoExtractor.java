package io.quarkiverse.googlecloudservices.it.logging;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logmanager.ExtLogRecord;

import io.quarkiverse.googlecloudservices.logging.runtime.TraceInfo;
import io.quarkiverse.googlecloudservices.logging.runtime.TraceInfoExtractor;

@ApplicationScoped
public class TestTraceInfoExtractor implements TraceInfoExtractor {

    @Override
    public TraceInfo extract(ExtLogRecord record) {
        return new TraceInfo(UUID.randomUUID().toString(), null);
    }
}
