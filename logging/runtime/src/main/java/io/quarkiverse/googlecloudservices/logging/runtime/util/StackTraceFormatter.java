package io.quarkiverse.googlecloudservices.logging.runtime.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Strings;

import io.quarkiverse.googlecloudservices.logging.runtime.LoggingConfiguration.StackElementRendering;

public class StackTraceFormatter {

    private final StackElementRendering format;

    public StackTraceFormatter(StackElementRendering format) {
        this.format = format;
    }

    public List<Map<String, ?>> format(Throwable th) {
        List<Map<String, ?>> root = new ArrayList<>(3);
        Set<Throwable> handled = Collections.newSetFromMap(new IdentityHashMap<>());
        append(root, handled, null, th, false);
        return root;
    }

    private void append(List<Map<String, ?>> root, Set<Throwable> handled, Throwable parent, Throwable th, boolean supressed) {
        if (th == null || th == parent) {
            return; // recursion ends
        } else {
            Map<String, Object> ex = new HashMap<>(3);
            ex.put("id", System.identityHashCode(th));
            ex.put("type", th.getClass().getName());
            putMessageIfNotEmpty(th, ex);
            root.add(ex);
            if (handled.contains(th)) {
                ex.put("reference", true);
                return; // recursion ends, with circular reference
            } else {
                handled.add(th);
                if (supressed) {
                    ex.put("supressed", true);
                }
                putStackFrames(ex, parent, th);
                for (Throwable s : th.getSuppressed()) {
                    append(root, handled, th, s, true);
                }
                append(root, handled, th, th.getCause(), false);
            }
        }
    }

    private void putStackFrames(Map<String, Object> ex, Throwable parent, Throwable th) {
        // common frame calculation from Throwable
        StackTraceElement[] trace = th.getStackTrace();
        StackTraceElement[] parentTrace = parent == null ? null : parent.getStackTrace();
        int m = trace.length - 1;
        int n = parentTrace == null ? -1 : parentTrace.length - 1;
        while (m >= 0 && n >= 0 && trace[m].equals(parentTrace[n])) {
            m--;
            n--;
        }
        int framesInCommon = trace.length - 1 - m;
        List<Object> list = new ArrayList<>(m + 1);
        for (int i = 0; i <= m; i++) {
            if (format == StackElementRendering.STRING) {
                list.add(renderFrameAsString(trace[i]));
            } else {
                list.add(renderFrameAsObject(trace[i]));
            }
        }
        ex.put("stack_trace", list);
        if (framesInCommon > 0) {
            ex.put("commonFrames", Integer.valueOf(framesInCommon));
        }
    }

    private Map<String, String> renderFrameAsObject(StackTraceElement e) {
        Map<String, String> element = new HashMap<>(3);
        element.put("class", e.getClassName());
        element.put("method", e.getMethodName());
        element.put("line", String.valueOf(e.getLineNumber()));
        return element;
    }

    private String renderFrameAsString(StackTraceElement e) {
        return String.format("%s.%s:%s", e.getClassName(), e.getMethodName(), String.valueOf(e.getLineNumber()));
    }

    private void putMessageIfNotEmpty(Throwable th, Map<String, Object> ex) {
        if (!Strings.isNullOrEmpty(th.getMessage())) {
            ex.put("message", th.getMessage());
        }
    }
}
