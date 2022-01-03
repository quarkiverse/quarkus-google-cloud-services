package io.quarkiverse.googlecloudservices.logging.runtime.util;

import java.util.logging.Level;

import com.google.cloud.logging.Severity;

public class LevelTransformer {

    private LevelTransformer() {
    }

    /**
     * Traslate JUL log level to a GCP logging severity.
     */
    public static Severity toSeverity(Level level) {
        int i = level.intValue();
        if (i <= Level.FINE.intValue()) {
            return Severity.DEBUG;
        } else if (i <= Level.INFO.intValue()) {
            return Severity.INFO;
        } else if (i <= Level.WARNING.intValue()) {
            return Severity.WARNING;
        } else if (i <= Level.SEVERE.intValue()) {
            return Severity.ERROR;
        } else if (i <= org.jboss.logmanager.Level.FATAL.intValue()) {
            return Severity.CRITICAL;
        } else {
            return Severity.DEFAULT;
        }
    }
}
