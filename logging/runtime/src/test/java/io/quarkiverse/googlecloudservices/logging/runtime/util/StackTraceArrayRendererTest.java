package io.quarkiverse.googlecloudservices.logging.runtime.util;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.googlecloudservices.logging.runtime.LoggingConfiguration.StackElementRendering;

public class StackTraceArrayRendererTest {

    @Test
    public void testTrivialCase() {
        Throwable th = captureThrowOne("message");
        List<Map<String, ?>> list = new StackTraceArrayRenderer(StackElementRendering.STRING).format(th);
        Assertions.assertEquals(1, list.size()); // a throwable is it's own cause
        Assertions.assertEquals("message", list.get(0).get("message"));
        Assertions.assertTrue(list.get(0).get("type").equals(RuntimeException.class.getName()));
    }

    @Test
    public void shouldHandleNullMessages() {
        Throwable th = captureThrowOne(null);
        List<Map<String, ?>> list = new StackTraceArrayRenderer(StackElementRendering.STRING).format(th);
        Assertions.assertFalse(list.get(0).containsKey("message"));
    }

    @Test
    public void shouldTruncateCommonFrames() {
        Throwable th = captureReThorwOne("message");
        List<Map<String, ?>> list = new StackTraceArrayRenderer(StackElementRendering.STRING).format(th);
        Assertions.assertEquals(2, list.size()); // with one cause
        Assertions.assertEquals("message", list.get(1).get("message"));
        Assertions.assertTrue(((Integer) list.get(1).get("commonFrames")).intValue() > 0);
    }

    private Throwable captureThrowOne(String msg) {
        try {
            throwOne(msg);
            return null;
        } catch (RuntimeException e) {
            return e;
        }
    }

    private Throwable captureReThorwOne(String msg) {
        try {
            reThrowOne(msg);
            return null;
        } catch (RuntimeException e) {
            return e;
        }
    }

    private void reThrowOne(String msg) {
        try {
            throwOne(msg);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    private void throwOne(String msg) {
        throw new RuntimeException(msg);
    }
}
