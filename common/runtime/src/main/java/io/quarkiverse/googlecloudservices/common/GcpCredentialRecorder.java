package io.quarkiverse.googlecloudservices.common;

import java.util.Optional;

import com.google.cloud.ServiceOptions;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class GcpCredentialRecorder {
    public void configure(GcpBootstrapConfiguration bootstrapConfig) {
        // This ack allows to set the projectId to its default at build time as we cannot use the
        // RunTimeConfigurationDefaultBuildItem that is only usable at build time.
        // A more elegant solution would be to create a config source for this.
        if (bootstrapConfig.enableMetadataServer && bootstrapConfig.projectId.isEmpty()) {
            bootstrapConfig.projectId = Optional.ofNullable(ServiceOptions.getDefaultProjectId());
        }

        ArcContainer container = Arc.container();
        container.instance(GcpConfigHolder.class).get().setBootstrapConfig(bootstrapConfig);
    }
}
