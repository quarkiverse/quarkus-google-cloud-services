package io.quarkiverse.googlecloudservices.it;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logmanager.ExtLogRecord;

import io.quarkiverse.googlecloudservices.logging.runtime.LabelExtractor;

@ApplicationScoped
public class TestLabelExtractor implements LabelExtractor {

    public Map<String, String> extract(ExtLogRecord record) {
        KeyValueParameter p = Arrays.asList(record.getParameters()).stream().filter(r -> (r instanceof KeyValueParameter))
                .map(r -> (KeyValueParameter) r).findFirst().orElse(null);
        return p == null ? Collections.emptyMap() : Collections.singletonMap(p.getKey(), p.getValue());
    }
}
