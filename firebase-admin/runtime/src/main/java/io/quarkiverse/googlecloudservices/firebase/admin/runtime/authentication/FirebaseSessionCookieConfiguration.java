package io.quarkiverse.googlecloudservices.firebase.admin.runtime.authentication;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.google.cloud.firebase.auth.session-cookie")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface FirebaseSessionCookieConfiguration {

    /**
     * Enable session cookie support
     */
    @WithDefault("false")
    boolean enabled();
}
