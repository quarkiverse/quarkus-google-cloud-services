package io.quarkiverse.googlecloudservices.storage.runtime;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.jboss.logmanager.ExtFormatter;
import org.jboss.logmanager.ExtLogRecord;

import com.google.common.base.Strings;

public class EscJsonFormatter {

    private static final Formatter MSG_FORMAT = new Formatter();

    public JsonFormatter toFormatter() {
        return (r) -> format(r);
    }

    protected Map<String, ?> format(ExtLogRecord record) {
        Map<String, Object> m = new HashMap<>();
        setEcsVersion(m);
        setTimestamp(m, record.getInstant());
        setLoggerName(m, record.getLoggerName());
        setThreadName(m, record.getThreadName());
        setThreadId(m, record.getThreadID());
        setFormattedMessage(m, record);
        setLogLevel(m, record.getLevel());
        setSource(m, record.getSourceClassName(), record.getSourceLineNumber(), record.getSourceMethodName());
        setMdc(m, record.getMdcCopy());
        setThrown(m, record.getThrown());
        setHost(m, record.getHostName());
        setParameters(m, record.getParameters());
        return m;
    }

    protected void setParameters(Map<String, Object> m, Object[] parameters) {
        if (parameters != null && parameters.length > 0) {
            List<String> list = (List<String>) m.computeIfAbsent("parameters", (k) -> new ArrayList<String>(parameters.length));
            for (Object o : parameters) {
                list.add(String.valueOf(o));
            }
        }
    }

    protected void setHost(Map<String, Object> m, String hostName) {
        if (!Strings.isNullOrEmpty(hostName)) {
            getOrCreateObject(m, "host").put("name", hostName);
        }
    }

    protected void setThrown(Map<String, Object> m, Throwable thrown) {
        if (thrown != null) {
            Map<String, Object> error = getOrCreateObject(m, "error");
            error.put("type", thrown.getClass().getName());
            String msg = thrown.getMessage();
            if (!Strings.isNullOrEmpty(msg)) {
                error.put("message", msg);
            }
            StringWriter sw = new StringWriter(1024);
            PrintWriter pw = new PrintWriter(sw);
            thrown.printStackTrace(pw);
            error.put("stack_trace", pw.toString());
        }
    }

    protected void setMdc(Map<String, Object> m, Map<String, String> mdcCopy) {
        if (mdcCopy != null && !mdcCopy.isEmpty()) {
            Map<String, Object> mdc = getOrCreateObject(m, "mdc");
            mdcCopy.forEach((k, v) -> mdc.put(k, v));
        }
    }

    protected void setSource(Map<String, Object> m, String sourceClassName, int sourceLineNumber, String sourceMethodName) {
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

    protected void setLogLevel(Map<String, Object> m, Level level) {
        getOrCreateObject(m, "log").put("level", level.getName());
    }

    protected void setFormattedMessage(Map<String, Object> m, ExtLogRecord record) {
        m.put("message", MSG_FORMAT.format(record));
    }

    protected void setThreadId(Map<String, Object> m, long longThreadID) {
        getOrCreateObject(getOrCreateObject(m, "process"), "thread").put("id", Long.valueOf(longThreadID));
    }

    protected void setThreadName(Map<String, Object> m, String threadName) {
        if (!Strings.isNullOrEmpty(threadName)) {
            getOrCreateObject(getOrCreateObject(m, "process"), "thread").put("name", threadName);
        }
    }

    protected void setLoggerName(Map<String, Object> m, String loggerName) {
        if (!Strings.isNullOrEmpty(loggerName)) {
            getOrCreateObject(m, "log").put("logger", loggerName);
        }
    }

    protected Map<String, Object> getOrCreateObject(Map<String, Object> m, String name) {
        return (Map<String, Object>) m.computeIfAbsent(name, (k) -> new HashMap<String, Object>(3));
    }

    protected void setEcsVersion(Map<String, Object> m) {
        getOrCreateObject(m, "ecs").put("version", "1.2.0");
    }

    protected void setTimestamp(Map<String, Object> m, Instant instant) {
        m.put("@timestamp", instant.atOffset(ZoneOffset.UTC).toString());
    }

    private static class Formatter extends ExtFormatter {

        @Override
        public String format(ExtLogRecord record) {
            return formatMessage(record);
        }
    }
}
