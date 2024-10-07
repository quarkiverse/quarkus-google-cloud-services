package io.quarkiverse.googlecloudservices.logging.runtime;

import java.util.Map;

import org.jboss.logmanager.ExtLogRecord;

/**
 * Bind an instance of this interface to include additional labels
 * in the log record. You should only bind one extractor in the CDI context.
 */
public interface LogRecordLabelExtractor {

    /**
     * Supply additional labels for a log record.
     *
     * @param record Record for which labels can be supplied, never null
     * @return a map of additional label values, may return null or empty but neither
     *         keys nor values therein should be null
     */
    Map<String, String> getCustomLabels(ExtLogRecord record);

}
