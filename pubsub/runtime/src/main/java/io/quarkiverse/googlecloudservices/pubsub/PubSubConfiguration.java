package io.quarkiverse.googlecloudservices.pubsub;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "quarkus.google.cloud.pubsub")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface PubSubConfiguration {
    /**
     * Enable emulator and set its host.
     */
    Optional<String> emulatorHost();

}
