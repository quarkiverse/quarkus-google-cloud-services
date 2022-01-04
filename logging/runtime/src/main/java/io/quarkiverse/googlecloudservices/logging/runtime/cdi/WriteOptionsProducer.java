package io.quarkiverse.googlecloudservices.logging.runtime.cdi;

import java.util.Collections;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.cloud.MonitoredResource;
import com.google.cloud.MonitoredResource.Builder;
import com.google.cloud.logging.Logging.WriteOption;

import io.quarkiverse.googlecloudservices.logging.runtime.LoggingConfiguration;
import io.quarkus.arc.Unremovable;

/**
 * Defautl write options producer.
 */
@Singleton
public class WriteOptionsProducer {

    @Inject
    LoggingConfiguration loggingConfig;

    @Default
    @Produces
    @Singleton
    @Unremovable
    public WriteOptionsHolder create() {
        return () -> new WriteOption[] {
                WriteOption.logName(loggingConfig.defaultLog),
                WriteOption.resource(createMonitoredResource()),
                WriteOption.labels(loggingConfig.defaultLabel == null ? Collections.emptyMap() : loggingConfig.defaultLabel)
        };
    }

    private MonitoredResource createMonitoredResource() {
        Builder b = MonitoredResource.newBuilder(loggingConfig.resource.type);
        if (loggingConfig.resource.label != null) {
            loggingConfig.resource.label.forEach((k, v) -> b.addLabel(k, v));
        }
        return b.build();
    }
}
