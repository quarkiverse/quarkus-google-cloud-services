package io.quarkiverse.googlecloudservices.logging.runtime.ecs;

import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.quarkiverse.googlecloudservices.logging.runtime.LoggingConfiguration;
import io.quarkiverse.googlecloudservices.logging.runtime.LoggingConfiguration.MDCConfig;
import io.quarkiverse.googlecloudservices.logging.runtime.LoggingConfiguration.ParametersConfig;
import io.quarkiverse.googlecloudservices.logging.runtime.LoggingConfiguration.StackTraceConfig;
import io.quarkiverse.googlecloudservices.logging.runtime.LoggingConfiguration.StructuredConfig;
import io.quarkiverse.googlecloudservices.logging.runtime.TraceInfo;

public class EscJsonFormatTest {

    @Test
    public void shouldReturnNullIfNotConfigured() {
        EscJsonFormat f = new EscJsonFormat();
        ExtLogRecord r = createNewLogRecord();
        TraceInfo i = createNewTraceInfo();
        Assertions.assertTrue(f.toEsc(r, i) == null);
    }

    @Test
    public void shouldSetEscVersionTimestampLogLevelAndMessage() {
        EscJsonFormat f = createConfiguredFormat();
        ExtLogRecord r = createNewLogRecord();
        JsonObject json = toJson(f, r);
        Assertions.assertEquals("1.2.0", json.getAsJsonObject("ecs").get("version").getAsString());
        Assertions.assertNotNull(json.get("@timestamp"));
        Assertions.assertEquals("INFO", json.getAsJsonObject("log").get("level").getAsString());
        Assertions.assertEquals("hello world", json.get("message").getAsString());
    }

    @Test
    public void loggerClassNameShouldTakePRecedenceOverLoggerName() {
        EscJsonFormat f = createConfiguredFormat();
        ExtLogRecord r = createNewLogRecord();
        r.setLoggerName("loggerName");
        JsonObject json = toJson(f, r);
        Assertions.assertEquals(getClass().getName(), json.getAsJsonObject("log").get("logger").getAsString());
        // set class name to null and make sure we get the logger name
        r = new ExtLogRecord(Level.INFO, "hello world", null);
        r.setLoggerName("loggerName");
        json = toJson(f, r);
        Assertions.assertEquals("loggerName", json.getAsJsonObject("log").get("logger").getAsString());
    }

    @Test
    public void testThreadNameAndId() {
        EscJsonFormat f = createConfiguredFormat();
        ExtLogRecord r = createNewLogRecord();
        r.setThreadID(0);
        r.setThreadName(null);
        JsonObject json = toJson(f, r);
        Assertions.assertTrue(json.get("process") == null);
        r.setThreadID(1);
        r.setThreadName("thread");
        json = toJson(f, r);
        Assertions.assertEquals("thread", json.getAsJsonObject("process").getAsJsonObject("thread").get("name").getAsString());
        Assertions.assertEquals(1, json.getAsJsonObject("process").getAsJsonObject("thread").get("id").getAsInt());
    }

    @Test
    public void testSourceClassLineAndMethod() {
        EscJsonFormat f = createConfiguredFormat();
        ExtLogRecord r = createNewLogRecord();
        r.setSourceClassName(null);
        JsonObject json = toJson(f, r);
        Assertions.assertTrue(json.getAsJsonObject("log").get("origin") == null);
        r.setSourceClassName(getClass().getName());
        r.setSourceLineNumber(67);
        r.setSourceMethodName("testSourceClassLineAndMethod");
        json = toJson(f, r);
        Assertions.assertEquals(getClass().getName(),
                json.getAsJsonObject("log").getAsJsonObject("origin").getAsJsonObject("class").get("name").getAsString());
        Assertions.assertEquals(67,
                json.getAsJsonObject("log").getAsJsonObject("origin").getAsJsonObject("class").get("line").getAsInt());
        Assertions.assertEquals("testSourceClassLineAndMethod",
                json.getAsJsonObject("log").getAsJsonObject("origin").get("function").getAsString());
    }

    @Test
    public void testExcludeMdcAccordingToConfig() {
        EscJsonFormat f = new EscJsonFormat();
        LoggingConfiguration c = createNewLoggingConfiguration();
        when(c.structured().mdc().included()).thenReturn(false);
        f.setLoggingConfiguration(c);
        ExtLogRecord r = createNewLogRecord();
        r.setMdc(Map.of("k", "v"));
        JsonObject json = toJson(f, r);
        Assertions.assertTrue(json.getAsJsonObject("mdc") == null);
    }

    @Test
    public void testMdcFieldName() {
        EscJsonFormat f = new EscJsonFormat();
        LoggingConfiguration c = createNewLoggingConfiguration();
        when(c.structured().mdc().fieldName()).thenReturn("fieldName");
        f.setLoggingConfiguration(c);
        ExtLogRecord r = createNewLogRecord();
        r.setMdc(Map.of("k", "v"));
        JsonObject json = toJson(f, r);
        Assertions.assertEquals("v", json.getAsJsonObject("fieldName").get("k").getAsString());
    }

    @Test
    public void testThrownTypeMessageAndStringStackTrace() {
        EscJsonFormat f = createConfiguredFormat();
        ExtLogRecord r = createNewLogRecord();
        r.setThrown(new Exception("hello exception"));
        JsonObject json = toJson(f, r);
        Assertions.assertEquals(Exception.class.getName(), json.getAsJsonObject("error").get("type").getAsString());
        Assertions.assertEquals("hello exception", json.getAsJsonObject("error").get("message").getAsString());
        // check that the trace is a string, the trace rendering itself is tested elsewhere
        Assertions.assertTrue(json.getAsJsonObject("error").get("stack_trace").isJsonPrimitive());
    }

    @Test
    public void testExcludeStackTrace() {
        EscJsonFormat f = new EscJsonFormat();
        LoggingConfiguration c = createNewLoggingConfiguration();
        when(c.structured().stackTrace().included()).thenReturn(false);
        f.setLoggingConfiguration(c);
        ExtLogRecord r = createNewLogRecord();
        r.setThrown(new Exception("hello exception"));
        JsonObject json = toJson(f, r);
        Assertions.assertTrue(json.getAsJsonObject("error").get("stack_trace") == null);
    }

    @Test
    public void testHost() {
        EscJsonFormat f = createConfiguredFormat();
        ExtLogRecord r = createNewLogRecord();
        r.setHostName(null);
        JsonObject json = toJson(f, r);
        Assertions.assertTrue(json.getAsJsonObject("host") == null);
        r.setHostName("host");
        json = toJson(f, r);
        Assertions.assertEquals("host", json.getAsJsonObject("host").get("name").getAsString());
    }

    @Test
    public void testExcludeParameters() {
        EscJsonFormat f = new EscJsonFormat();
        LoggingConfiguration c = createNewLoggingConfiguration();
        when(c.structured().parameters().included()).thenReturn(false);
        f.setLoggingConfiguration(c);
        ExtLogRecord r = createNewLogRecord();
        r.setParameters(new Object[] { "p1", "p2" });
        JsonObject json = toJson(f, r);
        Assertions.assertTrue(json.getAsJsonObject("parameters") == null);
    }

    @Test
    public void testIncludeParameters() {
        EscJsonFormat f = new EscJsonFormat();
        LoggingConfiguration c = createNewLoggingConfiguration();
        f.setLoggingConfiguration(c);
        ExtLogRecord r = createNewLogRecord();
        r.setParameters(new Object[] { "p1", "p2" });
        JsonObject json = toJson(f, r);
        Assertions.assertEquals("p2", json.getAsJsonArray("parameters").get(1).getAsString());
    }

    @Test
    public void shouldNotIncludeTraceIfNull() {
        EscJsonFormat f = createConfiguredFormat();
        ExtLogRecord r = createNewLogRecord();
        JsonObject json = toJson(f, r, null);
        Assertions.assertTrue(json.getAsJsonObject("trace") == null);
        Assertions.assertTrue(json.getAsJsonObject("span") == null);
    }

    @Test
    public void testTracing() {
        EscJsonFormat f = createConfiguredFormat();
        ExtLogRecord r = createNewLogRecord();
        TraceInfo i = createNewTraceInfo();
        JsonObject json = toJson(f, r, i);
        Assertions.assertEquals(i.getTraceId(), json.getAsJsonObject("trace").get("id").getAsString());
        Assertions.assertEquals(i.getSpanId(), json.getAsJsonObject("span").get("id").getAsString());
        // exclude span ID
        i = new TraceInfo(UUID.randomUUID().toString(), null);
        json = toJson(f, r, i);
        Assertions.assertTrue(json.getAsJsonObject("trace") != null);
        Assertions.assertTrue(json.getAsJsonObject("span") == null);
        // exclude trace ID
        i = new TraceInfo(null, UUID.randomUUID().toString());
        json = toJson(f, r, i);
        Assertions.assertTrue(json.getAsJsonObject("trace") == null);
        Assertions.assertTrue(json.getAsJsonObject("span") != null);
    }

    private JsonObject toJson(EscJsonFormat f, ExtLogRecord r) {
        return toJson(f, r, createNewTraceInfo());
    }

    private JsonObject toJson(EscJsonFormat f, ExtLogRecord r, TraceInfo i) {
        Map<String, ?> map = f.toEsc(r, i);
        JsonObject json = (JsonObject) new Gson().toJsonTree(map);
        return json;
    }

    private ExtLogRecord createNewLogRecord() {
        return new ExtLogRecord(Level.INFO, "hello world", getClass().getName());
    }

    private EscJsonFormat createConfiguredFormat() {
        EscJsonFormat f = new EscJsonFormat();
        f.setLoggingConfiguration(createNewLoggingConfiguration());
        return f;
    }

    private LoggingConfiguration createNewLoggingConfiguration() {
        LoggingConfiguration c = Mockito.mock(LoggingConfiguration.class);
        StructuredConfig structured = Mockito.mock(StructuredConfig.class);
        MDCConfig mdc = Mockito.mock(MDCConfig.class);
        StackTraceConfig stackTrace = Mockito.mock(StackTraceConfig.class);
        ParametersConfig parameters = Mockito.mock(ParametersConfig.class);

        when(mdc.included()).thenReturn(true);
        when(stackTrace.included()).thenReturn(true);
        when(parameters.included()).thenReturn(true);
        when(parameters.fieldName()).thenReturn("parameters");

        when(structured.mdc()).thenReturn(mdc);
        when(structured.stackTrace()).thenReturn(stackTrace);
        when(structured.parameters()).thenReturn(parameters);

        when(c.structured()).thenReturn(structured);

        return c;
    }

    private TraceInfo createNewTraceInfo() {
        return new TraceInfo(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }
}
