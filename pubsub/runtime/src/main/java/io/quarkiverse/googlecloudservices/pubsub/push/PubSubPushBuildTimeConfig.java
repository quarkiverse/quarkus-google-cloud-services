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
