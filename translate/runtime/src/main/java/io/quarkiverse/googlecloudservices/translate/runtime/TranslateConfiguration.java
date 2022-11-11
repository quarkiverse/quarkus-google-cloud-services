package io.quarkiverse.googlecloudservices.translate.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "google.cloud.translate", phase = ConfigPhase.RUN_TIME)
public class TranslateConfiguration {
    /**
     * Overrides the default service host.
     * This is most commonly used for development or testing activities with a local Google Cloud Translate emulator instance.
     */
    @ConfigItem
    public Optional<String> hostOverride;
}