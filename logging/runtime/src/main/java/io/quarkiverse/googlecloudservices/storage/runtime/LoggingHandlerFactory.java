package io.quarkiverse.googlecloudservices.storage.runtime;

import java.util.Optional;
import java.util.logging.Handler;

import org.jboss.logging.Logger;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class LoggingHandlerFactory {

    private static final Logger LOG = Logger.getLogger(LoggingHandlerFactory.class);

    public RuntimeValue<Optional<Handler>> create(LoggingConfiguration config) {
        if (!config.enabled) {
            LOG.info("Goocle Cloud logging is disabled");
            return new RuntimeValue<>(Optional.empty());
        } else {
            LOG.info("Intiating lazy logging handler for log: " + config.log);
            return new RuntimeValue<>(Optional.of(new LoggingHandler(config)));
        }
    }
}
