package io.quarkiverse.googlecloudservices.common;

import jakarta.inject.Singleton;

import io.quarkus.arc.Unremovable;

@Singleton
@Unremovable
public class GcpConfigHolder {
    private GcpBootstrapConfiguration bootstrapConfig;

    public GcpBootstrapConfiguration getBootstrapConfig() {
        return bootstrapConfig;
    }

    GcpConfigHolder setBootstrapConfig(GcpBootstrapConfiguration bootstrapConfig) {
        this.bootstrapConfig = bootstrapConfig;
        return this;
    }
}
