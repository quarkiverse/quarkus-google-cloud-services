package io.quarkiverse.googlecloudservices.logging.runtime.ecs;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.ErrorManager;
import java.util.logging.Level;

import org.jboss.logmanager.ExtFormatter;
import org.jboss.logmanager.ExtLogRecord;

import com.google.common.base.Strings;

import io.quarkiverse.googlecloudservices.logging.runtime.JsonFormatter;
import io.quarkiverse.googlecloudservices.logging.runtime.LoggingConfiguration;
import io.quarkiverse.googlecloudservices.logging.runtime.LoggingConfiguration.StackElementRendering;
import io.quarkiverse.googlecloudservices.logging.runtime.LoggingConfiguration.StackTraceRendering;

/**
 * This is the base class for the ESC json formatter. For small adjustments
 * such as parameter filtering, override this class, implement {@link org.jboss.logmanager.formatters.JsonFormatter}
 * and bind to CDI.
 */
public class EscJsonFormat {

    private static final Formatter MSG_FORMAT = new Formatter();

    public static JsonFormatter createFormatter() {
        return new JsonFormatter() {

            private EscJsonFormat formatter = new EscJsonFormat();

            @Override
            public Map<String, ?> format(ExtLogRecord record) {
                return formatter.toEsc(record);
            }

            @Override
            public void init(LoggingConfiguration config, ErrorManager errorManager) {
                formatter.setLoggingConfiguration(config);
                formatter.setErrorManager(errorManager);
            }
        };
    }

    protected LoggingConfiguration config;
    protected ErrorManager errorManager;

    public void setLoggingConfiguration(LoggingConfiguration config) {
        this.config = config;
    }

    public void setErrorManager(ErrorManager errorManager) {
        this.errorManager = errorManager;
    }

    public Map<String, ?> toEsc(ExtLogRecord record) {
        if (this.config == null) {
            return null; // sanity check
        } else {
            Map<String, Object> m = new HashMap<>();
            putEcsVersion(m);
            putTimestamp(m, record.getInstant());
            putLoggerName(m, record.getLoggerName());
            putThreadName(m, record.getThreadName());
            putThreadId(m, record.getThreadID());
            putFormattedMessage(m, record);
            putLogLevel(m, record.getLevel());
            putSource(m, record.getSourceClassName(), record.getSourceLineNumber(), record.getSourceMethodName());
            putThrown(m, record.getThrown());
            putHost(m, record.getHostName());
            putMdcIfEnabled(m, record.getMdcCopy());
            putParametersIfEnabled(m, record.getParameters());
            return m;
        }
    }

    @SuppressWarnings("unchecked")
    protected void putParametersIfEnabled(Map<String, Object> m, Object[] parameters) {
        if (parameters != null && parameters.length > 0 && this.config.parameters.included) {
            List<String> list = (List<String>) m.computeIfAbsent(this.config.parameters.fieldName,
                    (k) -> new ArrayList<String>(parameters.length));
            for (Object o : parameters) {
                if (shouldIncludeParameter(o)) {
                    list.add(String.valueOf(o));
                }
            }
        }
    }

    protected boolean shouldIncludeParameter(Object p) {
        return true;
    }

    protected void putHost(Map<String, Object> m, String hostName) {
        if (!Strings.isNullOrEmpty(hostName)) {
            getOrCreateObject(m, "host").put("name", hostName);
        }
    }

    protected void putThrown(Map<String, Object> m, Throwable thrown) {
        if (thrown != null) {
            Map<String, Object> error = getOrCreateObject(m, "error");
            error.put("type", thrown.getClass().getName());
            String msg = thrown.getMessage();
            if (!Strings.isNullOrEmpty(msg)) {
                error.put("message", msg);
            }
            if (this.config.stackTrace.included) {
                /*
                 * For string rendering we're using the normal stack trace, but
                 * for arrays we're only looking at first exception, not any causes
                 * since that would mean we'd have to look at possible circular references
                 */
                if (this.config.stackTrace.rendering == StackTraceRendering.STRING) {
                    // render as a standard out string
                    StringWriter sw = new StringWriter(1024);
                    PrintWriter pw = new PrintWriter(sw);
                    thrown.printStackTrace(pw);
                    pw.flush();
                    error.put("stack_trace", sw.toString());
                } else if (this.config.stackTrace.elementRendering == StackElementRendering.STRING) {
                    List<String> list = new ArrayList<>();
                    for (StackTraceElement e : thrown.getStackTrace()) {
                        list.add(String.format("%s.%s:%s", e.getClassName(), e.getMethodName(),
                                String.valueOf(e.getLineNumber())));
                    }
                    error.put("stack_trace", list);
                } else {
                    List<Map<String, String>> list = new ArrayList<>();
                    for (StackTraceElement e : thrown.getStackTrace()) {
                        Map<String, String> element = new HashMap<>(3);
                        element.put("class", e.getClassName());
                        element.put("method", e.getMethodName());
                        element.put("line", String.valueOf(e.getLineNumber()));
                        list.add(element);
                    }
                    error.put("stack_trace", list);
                }
            }
        }
    }

    protected void putMdcIfEnabled(Map<String, Object> m, Map<String, String> mdcCopy) {
        if (mdcCopy != null && !mdcCopy.isEmpty() && this.config.mdc.included) {
            Map<String, Object> mdc = getOrCreateObject(m, this.config.mdc.fieldName);
            mdcCopy.forEach((k, v) -> mdc.put(k, v));
        }
    }

    protected void putSource(Map<String, Object> m, String sourceClassName, int sourceLineNumber, String sourceMethodName) {
        if (!Strings.isNullOrEmpty(sourceClassName)) {
            Map<String, Object> log = getOrCreateObject(m, "log");
            Map<String, Object> origin = getOrCreateObject(log, "origin");
            Map<String, Object> clazz = getOrCreateObject(origin, "class");
            clazz.put("name", sourceClassName);
            clazz.put("line", Integer.valueOf(sourceLineNumber));
            if (!Strings.isNullOrEmpty(sourceMethodName)) {
                origin.put("function", sourceClassName);
            }
        }
    }

    protected void putLogLevel(Map<String, Object> m, Level level) {
        getOrCreateObject(m, "log").put("level", level.getName());
    }

    protected void putFormattedMessage(Map<String, Object> m, ExtLogRecord record) {
        m.put("message", MSG_FORMAT.format(record));
    }

    protected void putThreadId(Map<String, Object> m, long longThreadID) {
        getOrCreateObject(getOrCreateObject(m, "process"), "thread").put("id", Long.valueOf(longThreadID));
    }

    protected void putThreadName(Map<String, Object> m, String threadName) {
        if (!Strings.isNullOrEmpty(threadName)) {
            getOrCreateObject(getOrCreateObject(m, "process"), "thread").put("name", threadName);
        }
    }

    protected void putLoggerName(Map<String, Object> m, String loggerName) {
        if (!Strings.isNullOrEmpty(loggerName)) {
            getOrCreateObject(m, "log").put("logger", loggerName);
        }
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getOrCreateObject(Map<String, Object> m, String name) {
        return (Map<String, Object>) m.computeIfAbsent(name, (k) -> new HashMap<String, Object>(3));
    }

    protected void putEcsVersion(Map<String, Object> m) {
        getOrCreateObject(m, "ecs").put("version", "1.2.0");
    }

    protected void putTimestamp(Map<String, Object> m, Instant instant) {
        m.put("@timestamp", instant.atOffset(ZoneOffset.UTC).toString());
    }

    private static class Formatter extends ExtFormatter {

        @Override
        public String format(ExtLogRecord record) {
            return formatMessage(record);
        }
    }
}
