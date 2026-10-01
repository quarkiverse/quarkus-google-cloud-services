package io.quarkiverse.googlecloudservices.genai.runtime;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "quarkus.google.cloud.genai")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface GenAIConfiguration {
    /**
     * Gemini Developer API key.
     * When set, the client uses the Gemini Developer API instead of Vertex AI, unless `vertex-ai` is set to `true`.
     */
    Optional<String> apiKey();

    /**
     * Force the backend: `true` for Vertex AI, `false` for the Gemini Developer API.
     * Defaults to Vertex AI unless an API key is configured.
     */
    Optional<Boolean> vertexAi();

    /**
     * Google Cloud region used by Vertex AI, for example `global` or `us-central1`.
     */
    Optional<String> location();

    /**
     * Override the base URL of the API.
     */
    Optional<String> baseUrl();

    /**
     * Override the API version.
     */
    Optional<String> apiVersion();

    /**
     * Timeout of HTTP requests.
     */
    Optional<Duration> timeout();
}
