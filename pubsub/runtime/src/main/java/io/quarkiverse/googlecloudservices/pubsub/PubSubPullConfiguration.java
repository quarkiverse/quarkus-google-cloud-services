package io.quarkiverse.googlecloudservices.pubsub;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.google.cloud.pubsub.pull")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface PubSubPullConfiguration {

    /**
     * Number of concurrent streams to use for pull subscriptions. Defaults
     * to 1 which is the same as the base PubSub library.
     */
    @WithDefault("1")
    Optional<Integer> parallelStreamCount();

    /**
     * Number of concurrent messages to process per stream. Defaults
     * to 5 which is the same as the base PubSub library.
     */
    @WithDefault("5")
    Optional<Integer> streamConcurrency();

    default StreamConfig toStreamConfig() {
        return new StreamConfig(parallelStreamCount().orElse(1), streamConcurrency().orElse(5));
    }
}
