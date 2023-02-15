package io.quarkiverse.googlecloudservices.logging.runtime.util;

import org.jboss.logmanager.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.cloud.logging.Severity;

public class LevelTransformerTest {

    @Test
    public void testLevels() {
        // jboss logging
        Assertions.assertEquals(Severity.ERROR, LevelTransformer.toSeverity(Level.ERROR));
        Assertions.assertEquals(Severity.DEBUG, LevelTransformer.toSeverity(Level.DEBUG));
        Assertions.assertEquals(Severity.CRITICAL, LevelTransformer.toSeverity(Level.FATAL));
        Assertions.assertEquals(Severity.INFO, LevelTransformer.toSeverity(Level.INFO));
        Assertions.assertEquals(Severity.WARNING, LevelTransformer.toSeverity(Level.WARNING));
        // jul
        Assertions.assertEquals(Severity.ERROR, LevelTransformer.toSeverity(Level.SEVERE));
        Assertions.assertEquals(Severity.DEBUG, LevelTransformer.toSeverity(Level.FINE));
        Assertions.assertEquals(Severity.DEBUG, LevelTransformer.toSeverity(Level.FINER));
        Assertions.assertEquals(Severity.DEBUG, LevelTransformer.toSeverity(Level.FINEST));
        Assertions.assertEquals(Severity.INFO, LevelTransformer.toSeverity(Level.CONFIG));
    }
}
