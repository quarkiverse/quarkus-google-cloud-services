package io.quarkiverse.googlecloudservices.logging.runtime;

import java.util.Map;
import java.util.logging.ErrorManager;

import org.jboss.logmanager.ExtLogRecord;

import io.quarkiverse.googlecloudservices.logging.runtime.ecs.EscJsonFormat;

/**
 * Formatter for structured logging. You should only bind a single
 * formatter in the CDI context. If no formatter is found in the context
 * the {@link EscJsonFormat} is used.
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
     * @return A JSON map representation of the record, return null to drop the record
     */
    public Map<String, ?> format(ExtLogRecord record);

}
