package io.quarkiverse.googlecloudservices.firebase.admin.runtime;

import java.time.Duration;
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

        /**
         * Returns the session cookie configuration
         */
        SessionCookie sessionCookie();

        public interface SessionCookie {

            /**
             * Name to use for session cookies (see <a href="https://firebase.google.com/docs/auth/admin/manage-cookies#java_2">
             * Manage session cookies</a>)
             */
            @WithDefault("session")
            String name();

            /**
             * The expiration duration of the session cookie. Uses {@link java.time.Duration#parse(CharSequence)} to
             * get a duration for the expiration. See the JavaDoc of this method for the format of this value.
             * <p>
             * Defaults to 5 days.
             */
            @WithDefault("P5D")
            Duration expirationDuration();

            /**
             * Perform an additional check to see if the session was revoked
             */
            @WithDefault("true")
            Boolean checkRevoked();

            /**
             * Validate the expiration date of the token.
             */
            @WithDefault("false")
            Boolean validateToken();

            /**
             * Minimum token validity in case {@link #validateToken()} is set to true. Uses
             * {@link java.time.Duration#parse(CharSequence)} to get a duration for the expiration. See the JavaDoc of
             * this method for the format of this value.
             */
            Optional<Duration> minimumTokenValidity();

            /**
             * Path of an HTTP endpoint which can be used to perform the session login. If set, a reactive route will
             * be registered to handle setting the cookie based on an authenticated request.
             */
            Optional<String> loginApiPath();

            /**
             * Path of an HTTP endpoint which can be used to perform the session logout. If set, a reactive route will
             * be registered to handle clearing the session cookie.
             */
            Optional<String> logoutApiPath();

        }

    }

}
