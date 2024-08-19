package io.quarkiverse.googlecloudservices.firestore.runtime;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "quarkus.google.cloud.firestore")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface FirestoreConfiguration {
    /**
     * Overrides the default service host.
     * This is most commonly used for development or testing activities with a local Google Cloud Firestore emulator instance.
     */
    Optional<String> hostOverride();

    /**
     * Controls the retry settings for Firestore requests.
     */
    Optional<RetryConfiguration> retry();

    /**
     * The firestore database identifier.
     * It not set, the default will be used.
     */
    Optional<String> databaseId();

    interface RetryConfiguration {

        /**
         * Total timeout for all retries.
         */
        Optional<Duration> totalTimeout();

        /**
         * Delay before the first retry.
         */
        Optional<Duration> initialRetryDelay();

        /**
         * Controls the rate of change of the delay. Next retry is multiplied by this factor.
         */
        OptionalDouble retryDelayMultiplier();

        /**
         * Limits the maximum retry delay.
         */
        Optional<Duration> maxRetryDelay();

        /**
         * Determines the maximum number of attempts. When number of attempts reach this limit they stop retrying.
         */
        OptionalInt maxAttempts();

        /**
         * Timeout for the initial RPC.
         */
        Optional<Duration> initialRpcTimeout();

        /**
         * Controls the rate of change of the RPC timeout. Next timeout is multiplied by this factor.
         */
        OptionalDouble rpcTimeoutMultiplier();

        /**
         * Limits the maximum RPC timeout.
         */
        Optional<Duration> maxRpcTimeout();
    }

}
