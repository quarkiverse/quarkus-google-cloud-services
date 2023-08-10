package io.quarkiverse.googlecloudservices.secretmanager.runtime.config;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.Optional;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.google.cloud.ServiceOptions;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Converters;

public class SecretManagerConfigSourceFactory implements ConfigSourceFactory {
    @Override
    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
        ConfigValue enableMetadataServer = context.getValue("quarkus.google.cloud.enable-metadata-server");
        if (enableMetadataServer.getValue() != null) {
            if (Converters.getImplicitConverter(Boolean.class).convert(enableMetadataServer.getValue())) {
                String projectId = Optional.ofNullable(context.getValue("quarkus.google.cloud.project-id").getValue())
                        .orElse(ServiceOptions.getDefaultProjectId());
                if (projectId != null) {
                    return singletonList(new SecretManagerConfigSource(projectId));
                }
            }
        }
        return emptyList();
    }
}
