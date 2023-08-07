package io.quarkiverse.googlecloudservices.common;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.google.cloud.ServiceOptions;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Converters;
import io.smallrye.config.PropertiesConfigSource;

public class GcpDefaultsConfigSourceFactory implements ConfigSourceFactory {
    @Override
    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
        ConfigValue enableMetadataServer = context.getValue("quarkus.google.cloud.enable-metadata-server");
        if (enableMetadataServer.getValue() != null) {
            if (Converters.getImplicitConverter(Boolean.class).convert(enableMetadataServer.getValue())) {
                String defaultProjectId = ServiceOptions.getDefaultProjectId();
                if (defaultProjectId != null) {
                    return singletonList(
                            new PropertiesConfigSource(Map.of("quarkus.google.cloud.project-id", defaultProjectId),
                                    "GcpDefaultsConfigSource",
                                    -Integer.MAX_VALUE));
                }
            }
        }
        return emptyList();
    }
}
