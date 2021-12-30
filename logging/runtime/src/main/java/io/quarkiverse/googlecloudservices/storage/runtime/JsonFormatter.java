package io.quarkiverse.googlecloudservices.storage.runtime;

import java.util.Map;

import org.jboss.logmanager.ExtLogRecord;

@FunctionalInterface
public interface JsonFormatter {

    public Map<String, ?> format(ExtLogRecord record);

}
