package io.quarkiverse.googlecloudservices.firebase.database;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * Root configuration class for Google Cloud Firebase Realtime database setup.
 *
 */
@ConfigMapping(prefix = "quarkus.google.cloud.firebase.database")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface FirebaseDatabaseConfig {

    /**
     * Overrides the default service host.
     * This is most commonly used for development or testing activities with a local Firebase Realtime Database emulator
     * instance.
     */
    Optional<String> hostOverride();

}
