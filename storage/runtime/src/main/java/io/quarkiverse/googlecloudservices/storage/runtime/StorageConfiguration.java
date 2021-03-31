package io.quarkiverse.googlecloudservices.storage.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "google.cloud.storage", phase = ConfigPhase.RUN_TIME)
public class StorageConfiguration {
    /**
     * Overrides the default service host.
     * This is most commonly used for development or testing activities with a local Google Cloud Storage emulator instance.
     */
    @ConfigItem
    public Optional<String> hostOverride;
}