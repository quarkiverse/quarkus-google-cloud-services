package io.quarkiverse.googlecloudservices.it;

import java.util.Map;
import java.util.logging.ErrorManager;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logmanager.ExtLogRecord;

import io.quarkiverse.googlecloudservices.logging.runtime.JsonFormatter;
import io.quarkiverse.googlecloudservices.logging.runtime.LoggingConfiguration;
import io.quarkiverse.googlecloudservices.logging.runtime.ecs.EscJsonFormat;

@ApplicationScoped
public class TestFormatter extends EscJsonFormat implements JsonFormatter {

    @Override
    public void init(LoggingConfiguration config, ErrorManager error) {
        super.setLoggingConfiguration(config);
        super.setErrorManager(error);
    }

    @Override
    public Map<String, ?> format(ExtLogRecord record) {
        return super.toEsc(record);
    }

    @Override
    protected boolean shouldIncludeParameter(Object p) {
        return !(p instanceof KeyValueParameter);
    }
}
