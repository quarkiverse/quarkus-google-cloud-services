package io.quarkiverse.googlecloudservices.spanner.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "quarkus.google.cloud.spanner")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface SpannerConfiguration {
    /**
     * Enable emulator and set its host.
     */
    Optional<String> emulatorHost();
}
