package io.quarkiverse.googlecloudservices.firebase.admin.deployment.authentication;


import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "google.cloud.firebase.auth", phase = ConfigPhase.RUN_TIME)
public class FirebaseAuthConfiguration {

    /**
     * Enable or disable Firebase authentication.
     */
    @ConfigItem(defaultValue = "false")
    public boolean enabled;
}
