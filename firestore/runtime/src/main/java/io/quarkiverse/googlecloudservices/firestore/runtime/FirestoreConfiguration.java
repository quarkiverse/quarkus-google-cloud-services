package io.quarkiverse.googlecloudservices.firestore.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "google.cloud.firestore", phase = ConfigPhase.RUN_TIME)
public class FirestoreConfiguration {
    /**
     * Overrides the default service host.
     * This is most commonly used for development or testing activities with a local Google Cloud Firestore emulator instance.
     */
    @ConfigItem
    public Optional<String> hostOverride;
}
