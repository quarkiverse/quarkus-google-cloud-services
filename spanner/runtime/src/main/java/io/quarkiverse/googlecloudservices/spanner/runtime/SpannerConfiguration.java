package io.quarkiverse.googlecloudservices.spanner.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "google.cloud.spanner", phase = ConfigPhase.RUN_TIME)
public class SpannerConfiguration {
    /**
     * Enable emulator and set its host.
     */
    @ConfigItem
    public Optional<String> emulatorHost;
}
