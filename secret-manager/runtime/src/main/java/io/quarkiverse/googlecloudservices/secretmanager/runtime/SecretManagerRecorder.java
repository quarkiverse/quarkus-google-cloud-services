package io.quarkiverse.googlecloudservices.secretmanager.runtime;

import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import io.quarkiverse.googlecloudservices.common.GcpBootstrapConfiguration;
import io.quarkiverse.googlecloudservices.secretmanager.runtime.config.SecretManagerConfigSourceProvider;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class SecretManagerRecorder {
    public RuntimeValue<ConfigSourceProvider> configSources(GcpBootstrapConfiguration gcpBootstrapConfiguration) {
        return new RuntimeValue<>(
                new SecretManagerConfigSourceProvider(gcpBootstrapConfiguration));
    }
}
