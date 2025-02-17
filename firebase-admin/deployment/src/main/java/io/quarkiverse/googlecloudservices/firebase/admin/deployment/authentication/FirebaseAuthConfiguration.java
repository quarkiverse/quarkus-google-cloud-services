package io.quarkiverse.googlecloudservices.firebase.admin.deployment.authentication;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.google.cloud.firebase.auth")
@ConfigRoot
public interface FirebaseAuthConfiguration {

    /**
     * Enable or disable Firebase authentication.
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * Returns the session cookie configuration
     */
    SessionCookie sessionCookie();

    public interface SessionCookie {
        /**
         * Enable session cookie support
         */
        @WithDefault("false")
        boolean enabled();

    }
}
