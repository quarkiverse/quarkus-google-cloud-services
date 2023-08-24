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

    private static final String OPENTELEMETRY_CONTEXT_CONTEXT_STORAGE_PROVIDER_SYS_PROP = "io.opentelemetry.context.contextStorageProvider";

    @Override
    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
        ConfigValue enableMetadataServer = context.getValue("quarkus.google.cloud.enable-metadata-server");
        if (enableMetadataServer.getValue() != null) {
            if (Converters.getImplicitConverter(Boolean.class).convert(enableMetadataServer.getValue())) {
                String previousContextStorageSysProp = null;
                try {
                    // Google HTTP Client under the hood which attempts to record traces via OpenCensus which is wired
                    // to delegate to OpenTelemetry.
                    // This can lead to problems with the Quarkus OpenTelemetry extension which expects Vert.x to be running,
                    // something that is not the case at build time, see https://github.com/quarkusio/quarkus/issues/35500
                    previousContextStorageSysProp = System.setProperty(OPENTELEMETRY_CONTEXT_CONTEXT_STORAGE_PROVIDER_SYS_PROP,
                            "default");

                    String defaultProjectId = ServiceOptions.getDefaultProjectId();
                    if (defaultProjectId != null) {
                        return singletonList(
                                new PropertiesConfigSource(Map.of("quarkus.google.cloud.project-id", defaultProjectId),
                                        "GcpDefaultsConfigSource",
                                        -Integer.MAX_VALUE));
                    }
                } finally {
                    if (previousContextStorageSysProp == null) {
                        System.clearProperty(OPENTELEMETRY_CONTEXT_CONTEXT_STORAGE_PROVIDER_SYS_PROP);
                    } else {
                        System.setProperty(OPENTELEMETRY_CONTEXT_CONTEXT_STORAGE_PROVIDER_SYS_PROP,
                                previousContextStorageSysProp);
                    }
                }

            }
        }
        return emptyList();
    }
}
