package io.quarkiverse.googlecloudservices.logging.runtime;

import org.jboss.logmanager.ExtLogRecord;

/**
 * Bind an instance of this interface to include the current trace information
 * in the log record. You should only bind one extractor in the CDI context.
 */
public interface TraceInfoExtractor {

    /**
     * Extract trace information for a log record.
     *
     * @param record Record to extract info from, never null
     * @return Trace information, may return null
     */
    public TraceInfo extract(ExtLogRecord record);

}