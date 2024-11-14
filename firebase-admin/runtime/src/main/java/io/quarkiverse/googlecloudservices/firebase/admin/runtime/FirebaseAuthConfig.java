package io.quarkiverse.googlecloudservices.firebase.admin.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Root configuration class for Google Cloud Firebase Auth setup.
 * This interface provides a nested structure for configuration, including
 * a separate group for the development service configuration.
 * <p>
 * This interface mostly provides access to validate whether the Firebase Auth
 * Emulator is running.
 *
 */
@ConfigMapping(prefix = "quarkus.google.cloud.firebase.auth")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface FirebaseAuthConfig {

    /**
     * Sets the emulator host to use.
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