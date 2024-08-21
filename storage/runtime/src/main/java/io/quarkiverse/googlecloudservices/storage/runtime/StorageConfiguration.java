package io.quarkiverse.googlecloudservices.storage.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "quarkus.google.cloud.storage")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface StorageConfiguration {
    /**
     * Overrides the default service host.
     * This is most commonly used for development or testing activities with a local Google Cloud Storage emulator instance.
     */
    Optional<String> hostOverride();
}
