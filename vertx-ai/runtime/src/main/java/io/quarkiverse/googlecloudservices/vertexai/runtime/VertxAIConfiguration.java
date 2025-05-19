package io.quarkiverse.googlecloudservices.vertexai.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "quarkus.google.cloud.vertexai")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface VertxAIConfiguration {
    /**
     * Google Cloud region.
     */
    Optional<String> location();

    /**
     * Vertex AI API endpoint.
     */
    Optional<String> apiEndpoint();
}
