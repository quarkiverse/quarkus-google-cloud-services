package io.quarkiverse.googlecloudservices.firebase.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * Temporary Config root to retrieve the project id for the Firebase Emulator Container. We will remove this interface
 * in the future in favour of using the common setup.
 */
@ConfigMapping(prefix = "quarkus.google.cloud.devservices")
@ConfigRoot
public interface FirebaseDevServiceProjectConfig {

    /**
     * Google Cloud project ID. The project is required to be set if you use the Firebase Auth Dev service.
     */
    Optional<String> projectId();
}
