package io.quarkiverse.googlecloudservices.logging.runtime;

import java.util.Map;
import java.util.logging.ErrorManager;

import org.jboss.logmanager.ExtLogRecord;

/**
 * Formatter for structured logging.
 */
public interface JsonFormatter {

    /**
     * This method is called before the formatter is used.
     *
     * @param config The current config, never null
     */
    public void init(LoggingConfiguration config, ErrorManager errorManager);

    /**
     * Format a log record as a JSON map.
     *
     * @param record The record to format, never null
     * @param trace Trace information if available, may be null
     * @return A JSON map representation of the record, return null to drop the record
     */
    public Map<String, ?> format(ExtLogRecord record, TraceInfo trace);

}
