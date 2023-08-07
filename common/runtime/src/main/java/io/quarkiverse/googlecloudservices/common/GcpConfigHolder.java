package io.quarkiverse.googlecloudservices.common;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.arc.Unremovable;

@Singleton
@Unremovable
public class GcpConfigHolder {
    @Inject
    GcpBootstrapConfiguration bootstrapConfig;

    public GcpBootstrapConfiguration getBootstrapConfig() {
        return bootstrapConfig;
    }
}
