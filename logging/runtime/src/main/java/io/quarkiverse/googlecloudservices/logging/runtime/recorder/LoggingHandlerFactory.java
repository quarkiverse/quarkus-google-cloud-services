package io.quarkiverse.googlecloudservices.logging.runtime.recorder;

import java.util.Optional;
import java.util.logging.Handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkiverse.googlecloudservices.logging.runtime.LoggingConfiguration;
import io.quarkiverse.googlecloudservices.logging.runtime.LoggingHandler;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class LoggingHandlerFactory {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingHandlerFactory.class);

    public RuntimeValue<Optional<Handler>> create(LoggingConfiguration config) {
        if (!config.enabled()) {
            LOG.info("GCP logging is disabled");
            return new RuntimeValue<>(Optional.empty());
        } else {
            LOG.info("GCP logging handler created for default log: {}", config.defaultLog());
            return new RuntimeValue<>(Optional.of(new LoggingHandler(config)));
        }
    }
}
