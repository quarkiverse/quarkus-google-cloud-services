package io.quarkiverse.googlecloudservices.secretmanager.runtime.config;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.google.cloud.ServiceOptions;

import io.quarkiverse.googlecloudservices.common.GcpBootstrapConfiguration;
import io.quarkiverse.googlecloudservices.secretmanager.runtime.SecretManagerConfiguration;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class SecretManagerConfigSourceFactory implements ConfigSourceFactory {
    @Override
    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new ConfigSourceContext.ConfigSourceContextConfigSource(context))
                .withMapping(GcpBootstrapConfiguration.class)
                .withMapping(SecretManagerConfiguration.class)
                .withMappingIgnore("quarkus.**")
                .build();

        GcpBootstrapConfiguration gcpConfig = config.getConfigMapping(GcpBootstrapConfiguration.class);
        SecretManagerConfiguration secretManagerConfig = config.getConfigMapping(SecretManagerConfiguration.class);

        String projectId = getProjectId(gcpConfig);
        if (projectId != null) {
            return singletonList(new SecretManagerConfigSource(gcpConfig, secretManagerConfig, projectId));
        } else {
            return emptyList();
        }
    }

    private String getProjectId(GcpBootstrapConfiguration gcpConfig) {
        if (gcpConfig.enableMetadataServer()) {
            return gcpConfig.projectId().orElse(ServiceOptions.getDefaultProjectId());
        } else {
            return gcpConfig.projectId().orElse(null);
        }
    }
}
