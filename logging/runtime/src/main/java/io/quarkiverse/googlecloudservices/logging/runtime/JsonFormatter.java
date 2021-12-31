package io.quarkiverse.googlecloudservices.logging.runtime;

import java.util.Map;

import org.jboss.logmanager.ExtLogRecord;

public interface JsonFormatter {

    /**
     * This method is called before the formatter is used.
     * 
     * @param config The current config, never null
     */
    public void init(LoggingConfiguration config);

    /**
     * Format a log record as a JSON map.
     * 
     * @param record The record to format, never null
     * @return A JSON map representation of the record, should not be null
     */
    public Map<String, ?> format(ExtLogRecord record);

}
