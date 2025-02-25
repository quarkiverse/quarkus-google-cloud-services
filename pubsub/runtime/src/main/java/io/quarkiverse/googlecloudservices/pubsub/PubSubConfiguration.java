package io.quarkiverse.googlecloudservices.pubsub;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.google.cloud.pubsub")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface PubSubConfiguration {
    /**
     * Enable emulator and set its host.
     */
    Optional<String> emulatorHost();

    /**
     * Forces the usage of emulator credentials. The logic automatically uses emulator credentials in case
     * the emulatorHost is set.
     * <ul>
     * <li>If true: force usage of emulator credentials</li>
     * <li>If false: force not using emulator credentials</li>
     * </ul>
     */
    @WithDefault("true")
    boolean useEmulatorCredentials();

    /**
     * The pub sub push configuration
     */
    Push push();

    interface Push {

        /**
         * The endpoint path for the pubsub push calls.
         */
        Optional<String> endpointPath();

        /**
         * Audiences to accept for pubsub push messages. This can be set as a comma-separated list for multiple
         * audiences. If the audience is not configured, the <code>aud</code> claim in the Pub sub JWT will be ignored
         */
        Optional<String> audience();

        /**
         * In case this is set, a query-parameter called "token" is expected to be present in the request with the same
         * value as this configuration option. Calls without this token will be denied. If this property is not set
         * the token will be ignored.
         */
        Optional<String> verificationToken();

        /**
         * Email adddress of the service account used to send the pub-sub messages. The JWT used for authentication will
         * contain this email address when Google PubSub calls the push-endpoint.
         */
        Optional<String> serviceAccountEmail();
    }
}
