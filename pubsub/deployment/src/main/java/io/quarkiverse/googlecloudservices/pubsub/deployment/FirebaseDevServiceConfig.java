package io.quarkiverse.googlecloudservices.pubsub.deployment;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

import java.util.Optional;

/**
 * Config mapping to detect if the Firebase Dev Services are running, in which case the PubSub dev service
 * will be disabled by default as these two Devservice are in conflict with each other.
 */
@ConfigMapping(prefix = "quarkus.google.cloud.firebase.devservice")
@ConfigRoot
public interface FirebaseDevServiceConfig {

    /**
     * Indicates to use the dev service for Firebase. The default value is not setup unless the firebase module
     * is included. In that case, the Firebase devservices will by default be preferred and the DevService for
     * PubSub will be disabled.
     */
    Optional<Boolean> preferFirebaseDevServices();

}
