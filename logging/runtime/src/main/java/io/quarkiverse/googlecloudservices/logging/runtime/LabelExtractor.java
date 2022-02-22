package io.quarkiverse.googlecloudservices.logging.runtime;

import java.util.Map;

import org.jboss.logmanager.ExtLogRecord;

/**
 * This interface can be implemented to extract extra labels
 * for a structured record. You can bind multiple extractors and they
 * will be called sequentially.
 */
public interface LabelExtractor {

    /**
     * Extract extra labels for a log record. These labels will
     * overwrite any default labels with the same key.
     * 
     * @param record Record to process, never null
     * @return A map of labels, return empy map or null for no extra labels
     */
    public Map<String, String> extract(ExtLogRecord record);

}
