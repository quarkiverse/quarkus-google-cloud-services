package io.quarkiverse.googlecloudservices.firebase.admin.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Root configuration class for Google Cloud Firebase Auth setup.
 * <p>
 * This interface mostly provides access to validate whether the Firebase Auth
 * Emulator is running.
 *
 */
@ConfigMapping(prefix = "quarkus.google.cloud.firebase")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface FirebaseAuthConfig {

    /**
     * Returns the auth configuration
     */
    AuthConfig auth();

    public interface AuthConfig {
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

        /**
         * When set, the values in this claim in the Firebase JWT will be mapped to the roles in the Quarkus
         * {@link io.quarkus.security.identity.SecurityIdentity}. This claim can either be a set of roles
         * (i.e. an array in the JWT) or a single value.
         */
        Optional<String> rolesClaim();

    }

}
