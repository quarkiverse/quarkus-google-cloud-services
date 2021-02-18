package io.quarkiverse.googlecloudservices.secretmanager.runtime.config;

import java.util.Collections;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.google.cloud.ServiceOptions;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;

public class SecretManagerConfigSourceFactory implements ConfigSourceFactory {

    private static final String PROJECT_ID_PROPERTY = "quarkus.google.cloud.project-id";

    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext configSourceContext) {
        String projectId = configSourceContext.getValue(PROJECT_ID_PROPERTY).getValue();
        if (projectId == null) {
            // Fallback to the default project detected via Application Default Credentials.
            projectId = ServiceOptions.getDefaultProjectId();
        }

        return Collections.singletonList(new SecretManagerConfigSource(projectId));
    }
}
