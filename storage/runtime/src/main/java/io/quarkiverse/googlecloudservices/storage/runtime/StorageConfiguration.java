package io.quarkiverse.googlecloudservices.storage.runtime;

import java.util.Optional;

import org.threeten.bp.Duration;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.google.cloud.storage")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface StorageConfiguration {
    /**
     * Overrides the default service host.
     * This is most commonly used for development or testing activities with a local Google Cloud Storage emulator instance.
     */
    Optional<String> hostOverride();

    /**
     * Transport options
     */
    Transport transport();

    /**
     * Retry options
     */
    Retry retry();

    interface Transport {

        /**
         * Use the GRPC transport instead of the default HTTP transport.
         */
        @WithDefault("false")
        Boolean useGrpc();

        /**
         * Storage HTTP transport connect timeout
         */
        Optional<Integer> httpConnectTimeout();

        /**
         * Storage transport read timeout
         */
        Optional<Integer> httpReadTimeout();
    }

    interface Retry {

        /**
         * Initial retry delay. See {@link com.google.api.gax.retrying.RetrySettings.Builder#setInitialRetryDelay(Duration)}
         */
        Optional<Integer> initialRetryDelayMillis();

        /**
         * Max retry delay. See {@link com.google.api.gax.retrying.RetrySettings.Builder#setMaxRetryDelay(Duration)}
         */
        Optional<Integer> maxRetryDelayMillis();

        /**
         * Initial RPC timeout. See {@link com.google.api.gax.retrying.RetrySettings.Builder#setInitialRpcTimeout(Duration)}
         */
        Optional<Integer> initialRpcTimeoutMillis();

        /**
         * Max RPC timeout. See {@link com.google.api.gax.retrying.RetrySettings.Builder#setMaxRpcTimeout(Duration)}
         */
        Optional<Integer> maxRpcTimeoutMillis();

        /**
         * Logical timeout. See {@link com.google.api.gax.retrying.RetrySettings.Builder#setLogicalTimeout(Duration)}
         */
        Optional<Integer> logicalTimeoutMillis();

        /**
         * Total timeout. See {@link com.google.api.gax.retrying.RetrySettings.Builder#setTotalTimeout(Duration)}
         */
        Optional<Integer> totalTimeoutMillis();

        /**
         * Maximum attempts. See {@link com.google.api.gax.retrying.RetrySettings.Builder#setMaxAttempts(int)}
         */
        Optional<Integer> maxAttempts();

    }
}
