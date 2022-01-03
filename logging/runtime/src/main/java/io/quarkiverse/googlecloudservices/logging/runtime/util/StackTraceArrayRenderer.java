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

/**
 * This is a utility class for rendering a stack trace as an array
 * on thrown exceptions. It uses the same calculation as {@link Throwable}
 * for truncating stacks for causes - i.e. not show the same stack frames when
 * they are common - and checks for self referencing and circular references. 
 */
public class StackTraceArrayRenderer {

    private final StackElementRendering format;

    public StackTraceArrayRenderer(StackElementRendering format) {
        this.format = format;
    }

    /**
     * The returned list will contain one object for each throwable (including
     * causes and supressed exceptions), and each object will contain a list of
     * stack frames expressed as defined by the {@link StackElementRendering} given
     * in the constructor. 
     * 
     * @param th Throwable to format, must not be nnull
     * @return A list of exceptions as JSON objects
     */
    public List<Map<String, ?>> format(Throwable th) {
        List<Map<String, ?>> root = new ArrayList<>(3);
        // use identity based map to check for circular references
        Set<Throwable> handled = Collections.newSetFromMap(new IdentityHashMap<>());
        append(root, handled, null, th, false);
        return root;
    }

    /*
     * Append the "throwable" to the "root" list of exceptions. The parent will be null for the 
     * first exception. This is a recorsive method and "throwable" might be null (for a null cause).
     */
    private void append(List<Map<String, ?>> root, Set<Throwable> handled, Throwable parent, Throwable throwable, boolean supressed) {
        if (throwable == null || throwable == parent) {
            return; // recursion ends
        } else {
            Map<String, Object> ex = new HashMap<>(3);
            ex.put("id", System.identityHashCode(throwable));
            ex.put("type", throwable.getClass().getName());
            putMessageIfNotEmpty(throwable, ex);
            root.add(ex);
            if (handled.contains(throwable)) {
                // recursion ends with circular reference
                ex.put("reference", true);
                return; 
            } else {
                handled.add(throwable);
                if (supressed) {
                    ex.put("supressed", true);
                }
                putStackFrames(ex, parent, throwable);
                for (Throwable s : throwable.getSuppressed()) {
                    append(root, handled, throwable, s, true);
                }
                append(root, handled, throwable, throwable.getCause(), false);
            }
        }
    }

    private void putStackFrames(Map<String, Object> ex, Throwable parent, Throwable th) {
        // common frame calculation from Throwable
        StackTraceElement[] trace = th.getStackTrace();
        StackTraceElement[] parentTrace = parent == null ? null : parent.getStackTrace();
        int m = trace.length - 1;
        // if parent is null, set n = -1 and which will bypass the loop
        int n = parentTrace == null ? -1 : parentTrace.length - 1;
        while (m >= 0 && n >= 0 && trace[m].equals(parentTrace[n])) {
            m--;
            n--;
        }
        // framesInCommon might be negative if parent is null
        int framesInCommon = trace.length - 1 - m;
        // render frames
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
