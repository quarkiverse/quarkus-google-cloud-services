package io.quarkiverse.googlecloudservices.common;

import java.util.Optional;

import com.google.cloud.ServiceOptions;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "google.cloud", phase = ConfigPhase.RUN_TIME)
public class GcpConfiguration {
    /**
     * Google Cloud project ID.
     * It defaults to `ServiceOptions.getDefaultProjectId()`, so to the project ID corresponding to the default credentials.
     */
    @ConfigItem
    public String projectId;

    /**
     * Google Cloud service account file location.
     */
    @ConfigItem
    public Optional<String> serviceAccountLocation;

    /**
     * Google Cloud service account file encoded in base64.
     */
    @ConfigItem
    public Optional<String> serviceAccountEncodedKey;

    /**
     * Enable Google Cloud access token authentication
     * For example, the access token which is returned as part of OpenId Connect Authorization Code Flow
     * may be used to access Google Cloud services on behalf of the authenticated user.
     *
     * Note that if a service account location is configured then the access token will be ignored even if this property is
     * enabled.
     *
     * Disable this property if the default Google Cloud authentication is required.
     */
    @ConfigItem(defaultValue = "true")
    public boolean accessTokenEnabled = true;
}
