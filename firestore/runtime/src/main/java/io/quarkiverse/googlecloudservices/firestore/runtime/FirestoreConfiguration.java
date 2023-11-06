package io.quarkiverse.googlecloudservices.firestore.runtime;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
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

    /**
     * Controls the retry settings for Firestore requests.
     */
    @ConfigItem
    public Optional<RetryConfiguration> retry;

    /**
     * The firestore database identifier.
     * It not set, the default will be used.
     */
    @ConfigItem
    public Optional<String> databaseId;

    @ConfigGroup
    public static class RetryConfiguration {

        /**
         * Total timeout for all retries.
         */
        @ConfigItem
        public Optional<Duration> totalTimeout;

        /**
         * Delay before the first retry.
         */
        @ConfigItem
        public Optional<Duration> initialRetryDelay;

        /**
         * Controls the rate of change of the delay. Next retry is multiplied by this factor.
         */
        @ConfigItem
        public OptionalDouble retryDelayMultiplier;

        /**
         * Limits the maximum retry delay.
         */
        @ConfigItem
        public Optional<Duration> maxRetryDelay;

        /**
         * Determines the maximum number of attempts. When number of attempts reach this limit they stop retrying.
         */
        @ConfigItem
        public OptionalInt maxAttempts;

        /**
         * Timeout for the initial RPC.
         */
        @ConfigItem
        public Optional<Duration> initialRpcTimeout;

        /**
         * Controls the rate of change of the RPC timeout. Next timeout is multiplied by this factor.
         */
        @ConfigItem
        public OptionalDouble rpcTimeoutMultiplier;

        /**
         * Limits the maximum RPC timeout.
         */
        @ConfigItem
        public Optional<Duration> maxRpcTimeout;
    }

}
