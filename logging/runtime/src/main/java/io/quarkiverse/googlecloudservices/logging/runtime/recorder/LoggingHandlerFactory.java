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

    private Logger log = LoggerFactory.getLogger(getClass());

    public RuntimeValue<Optional<Handler>> create(LoggingConfiguration config) {
        if (!config.enabled) {
            log.info("Goocle Cloud logging is disabled");
            return new RuntimeValue<>(Optional.empty());
        } else {
            log.info("Intiating lazy logging handler for log: {}", config.log);
            return new RuntimeValue<>(Optional.of(new LoggingHandler(config)));
        }
    }
}
