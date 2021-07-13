package io.quarkiverse.googlecloudservices.common;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class GcpCredentialRecorder {
    public void configure(GcpBootstrapConfiguration bootstrapConfig) {
        ArcContainer container = Arc.container();
        container.instance(GcpConfigHolder.class).get().setBootstrapConfig(bootstrapConfig);
    }
}
