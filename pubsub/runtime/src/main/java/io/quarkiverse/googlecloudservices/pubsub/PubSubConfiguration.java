package io.quarkiverse.googlecloudservices.pubsub;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.google.cloud.pubsub")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface PubSubConfiguration {
    /**
     * Enable emulator and set its host.
     */
    Optional<String> emulatorHost();

    /**
     * Forces the usage of emulator credentials. The logic automatically uses emulator credentials in case
     * the emulatorHost is set.
     * <ul>
     * <li>If true: force usage of emulator credentials</li>
     * <li>If false: force not using emulator credentials</li>
     * </ul>
     */
    @WithDefault("true")
    boolean useEmulatorCredentials();
}
