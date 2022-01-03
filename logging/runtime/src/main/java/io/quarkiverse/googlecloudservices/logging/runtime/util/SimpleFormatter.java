package io.quarkiverse.googlecloudservices.logging.runtime.util;

import org.jboss.logmanager.ExtFormatter;
import org.jboss.logmanager.ExtLogRecord;

/**
 * This formatter uses the {@link ExtFormatter} without
 * any modifications. 
 */
public class SimpleFormatter extends ExtFormatter {

    @Override
    public String format(ExtLogRecord record) {
        return super.formatMessage(record);
    }
}
