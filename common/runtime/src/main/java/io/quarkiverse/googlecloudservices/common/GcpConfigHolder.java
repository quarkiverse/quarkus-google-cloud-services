package io.quarkiverse.googlecloudservices.common;

import io.quarkus.arc.Unremovable;

import javax.inject.Singleton;

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
