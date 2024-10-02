package io.quarkiverse.googlecloudservices.logging.runtime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.google.cloud.logging.Logging;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;

/**
 * Unit tests for the {@link LoggingHandler} class.
 */
class LoggingHandlerTest {

    private final PrintStream standardOut = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

    @BeforeEach
    void setUp() {
        // This enables us to capture anything written via System.out.println during a test
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @AfterEach
    void tearDown() {
        // Reset System.out after test complete
        System.setOut(standardOut);
    }

    @Test
    void shouldLogToStdoutWithTraceInfoAndLabels() {
        String traceId = UUID.randomUUID().toString();
        String spanId = UUID.randomUUID().toString();
        String msg = "hello world";
        String projectId = "my-project";
        int sourceLineNumber = 101;

        ArcContainer container = createArcContainer(traceId, spanId);

        try (MockedStatic<Arc> arc = Mockito.mockStatic(Arc.class)) {
            arc.when(Arc::container).thenReturn(container);

            try (LoggingHandler h = new LoggingHandler(createJsonStdoutLoggingConfiguration(projectId))) {
                // Publish a log record
                h.doPublish(createNewInfoLogRecord(msg, sourceLineNumber));
            }

            // Assert on what was written to System.out
            final String logEntry = outputStreamCaptor.toString();
            assertNotNull(logEntry);
            JsonObject logEntryJson = JsonParser.parseString(logEntry).getAsJsonObject();
            assertAll(
                    () -> assertEquals("INFO", logEntryJson.get("severity").getAsString()),
                    () -> assertNotNull(logEntryJson.get("time").getAsString()),
                    () -> assertNotNull(logEntryJson.get("@timestamp").getAsString()),
                    () -> assertEquals(msg, logEntryJson.get("message").getAsString()),
                    () -> assertEquals(String.format("projects/%s/traces/%s", projectId, traceId),
                            logEntryJson.get("logging.googleapis.com/trace").getAsString()),
                    () -> assertEquals(spanId, logEntryJson.get("logging.googleapis.com/spanId").getAsString()),
                    () -> assertTrue(logEntryJson.get("logging.googleapis.com/trace_sampled").getAsBoolean()));

            JsonObject log = logEntryJson.get("log").getAsJsonObject();
            assertNotNull(log);
            assertAll(
                    () -> assertEquals("INFO", log.get("level").getAsString()),
                    () -> assertEquals(getClass().getName(), log.get("logger").getAsString()));

            JsonObject origin = log.get("origin").getAsJsonObject();
            assertNotNull(origin);
            JsonObject classOrigin = origin.get("class").getAsJsonObject();
            assertNotNull(classOrigin);
            assertEquals(getClass().getName(), classOrigin.get("name").getAsString());
            assertEquals(String.format("%s.0", sourceLineNumber), classOrigin.get("line").getAsNumber().toString());
        }

    }

    private ArcContainer createArcContainer(String traceId, String spanId) {
        ArcContainer container = Mockito.mock(ArcContainer.class);
        InstanceHandle<TraceInfoExtractor> traceInfoInstanceHandler = Mockito.mock(InstanceHandle.class);
        when(traceInfoInstanceHandler.get()).thenReturn(x -> new TraceInfo(traceId, spanId));
        when(traceInfoInstanceHandler.isAvailable()).thenReturn(true);
        when(container.instance(TraceInfoExtractor.class)).thenReturn(traceInfoInstanceHandler);

        InstanceHandle<Logging> loggingInstanceHandler = Mockito.mock(InstanceHandle.class);
        Logging logging = Mockito.mock(Logging.class);
        when(loggingInstanceHandler.get()).thenReturn(logging);
        when(container.instance(Logging.class)).thenReturn(loggingInstanceHandler);
        return container;
    }

    private ExtLogRecord createNewInfoLogRecord(String msg, int sourceLineNumber) {
        ExtLogRecord extLogRecord = new ExtLogRecord(Level.INFO, msg, getClass().getName());
        extLogRecord.setSourceClassName(getClass().getName());
        extLogRecord.setSourceLineNumber(sourceLineNumber);
        return extLogRecord;
    }

    private LoggingConfiguration createJsonStdoutLoggingConfiguration(String projectId) {
        LoggingConfiguration c = Mockito.mock(LoggingConfiguration.class);

        LoggingConfiguration.MDCConfig mdc = Mockito.mock(LoggingConfiguration.MDCConfig.class);
        when(mdc.included()).thenReturn(true);

        LoggingConfiguration.StackTraceConfig stackTrace = Mockito.mock(LoggingConfiguration.StackTraceConfig.class);
        when(stackTrace.included()).thenReturn(true);

        LoggingConfiguration.ParametersConfig parameters = Mockito.mock(LoggingConfiguration.ParametersConfig.class);
        when(parameters.included()).thenReturn(true);
        when(parameters.fieldName()).thenReturn("parameters");

        LoggingConfiguration.StructuredConfig structured = Mockito.mock(LoggingConfiguration.StructuredConfig.class);
        when(structured.mdc()).thenReturn(mdc);
        when(structured.stackTrace()).thenReturn(stackTrace);
        when(structured.parameters()).thenReturn(parameters);
        when(c.structured()).thenReturn(structured);

        LoggingConfiguration.GcpTracingConfig gcpTracingConfig = Mockito.mock(LoggingConfiguration.GcpTracingConfig.class);
        when(gcpTracingConfig.enabled()).thenReturn(true);
        when(gcpTracingConfig.projectId()).thenReturn(Optional.of(projectId));
        when(c.gcpTracing()).thenReturn(gcpTracingConfig);

        LoggingConfiguration.ResourceConfig resourceConfig = Mockito.mock(LoggingConfiguration.ResourceConfig.class);
        when(resourceConfig.type()).thenReturn("generic_node");
        when(c.resource()).thenReturn(resourceConfig);

        when(c.format()).thenReturn(LoggingConfiguration.LogFormat.JSON);
        when(c.logTarget()).thenReturn(com.google.cloud.logging.LoggingHandler.LogTarget.STDOUT);

        return c;
    }
}
