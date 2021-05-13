package io.quarkiverse.googlecloudservices.common;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Bootstrap configuration
 *
 * We need the projectId at bootstrap to be able to register the secret manager config source that needs it.
 */
@ConfigRoot(name = "google.cloud", phase = ConfigPhase.BOOTSTRAP)
public class GcpBootstrapConfiguration {
    /**
     * Google Cloud project ID.
     * It defaults to `ServiceOptions.getDefaultProjectId()`, so to the project ID corresponding to the default credentials.
     */
    @ConfigItem
    public String projectId;
}
