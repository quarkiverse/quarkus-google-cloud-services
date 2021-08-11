package io.quarkiverse.googlecloudservices.secretmanager.runtime.config;

import java.util.Collections;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import com.google.cloud.ServiceOptions;

import io.quarkiverse.googlecloudservices.common.GcpBootstrapConfiguration;

public class SecretManagerConfigSourceProvider implements ConfigSourceProvider {

    private final String projectId;

    public SecretManagerConfigSourceProvider(GcpBootstrapConfiguration gcpBootstrapConfiguration) {
        this.projectId = gcpBootstrapConfiguration.projectId.orElse(null);
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        String projectId = this.projectId == null ? ServiceOptions.getDefaultProjectId() : this.projectId;
        return Collections.singletonList(new SecretManagerConfigSource(projectId));
    }
}
