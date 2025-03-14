package io.quarkiverse.googlecloudservices.pubsub.push;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.google.cloud.pubsub.push")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface PubSubPushBuildTimeConfig {

    /**
     * Enable push configuration
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * The endpoint path for the pubsub push calls.
     */
    Optional<String> endpointPath();

}
