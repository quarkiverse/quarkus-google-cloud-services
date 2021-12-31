package io.quarkiverse.googlecloudservices.logging.runtime;

import java.util.Map;

import org.jboss.logmanager.ExtLogRecord;

@FunctionalInterface
public interface JsonFormatter {

    /**
     * 
     * @param record The record to format, never null
     * @return A JSON map representation of the record, should not be null
     */
    public Map<String, ?> format(ExtLogRecord record);

}
