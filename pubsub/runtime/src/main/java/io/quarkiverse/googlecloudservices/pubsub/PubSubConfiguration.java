package io.quarkiverse.googlecloudservices.pubsub;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "google.cloud.pubsub", phase = ConfigPhase.RUN_TIME)
public class PubSubConfiguration {
    /**
     * Enable emulator and set its host.
     */
    @ConfigItem
    public Optional<String> emulatorHost;

}
