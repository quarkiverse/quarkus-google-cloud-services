package io.quarkiverse.googlecloudservices.genai.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.genai.Client;
import com.google.genai.types.HttpOptions;

import io.quarkiverse.googlecloudservices.common.GcpConfigHolder;
import io.quarkus.arc.Unremovable;

@ApplicationScoped
public class GenAIProducer {
    @Inject
    Instance<Credentials> googleCredentials;

    @Inject
    GcpConfigHolder gcpConfigHolder;

    @Inject
    GenAIConfiguration genAIConfiguration;

    @Produces
    @Singleton
    @Default
    @Unremovable
    public Client client() {
        boolean vertexAI = genAIConfiguration.vertexAi().orElseGet(() -> genAIConfiguration.apiKey().isEmpty());

        if (vertexAI && genAIConfiguration.apiKey().isPresent()) {
            throw new IllegalStateException(
                    "quarkus.google.cloud.genai.api-key cannot be used with Vertex AI, "
                            + "remove it or set quarkus.google.cloud.genai.vertex-ai=false");
        }

        var builder = Client.builder().vertexAI(vertexAI);

        if (vertexAI) {
            Credentials credentials = googleCredentials.get();
            if (!(credentials instanceof GoogleCredentials googleCreds)) {
                throw new IllegalStateException(
                        "The Gen AI SDK requires GoogleCredentials but the Credentials bean is of type "
                                + credentials.getClass().getName());
            }
            builder.credentials(googleCreds);
            gcpConfigHolder.getBootstrapConfig().projectId().ifPresent(builder::project);
            genAIConfiguration.location().ifPresent(builder::location);
        } else {
            genAIConfiguration.apiKey().ifPresent(builder::apiKey);
        }

        if (genAIConfiguration.baseUrl().isPresent() || genAIConfiguration.apiVersion().isPresent()
                || genAIConfiguration.timeout().isPresent()) {
            var httpOptions = HttpOptions.builder();
            genAIConfiguration.baseUrl().ifPresent(httpOptions::baseUrl);
            genAIConfiguration.apiVersion().ifPresent(httpOptions::apiVersion);
            genAIConfiguration.timeout().ifPresent(t -> httpOptions.timeout((int) Math.min(t.toMillis(), Integer.MAX_VALUE)));
            builder.httpOptions(httpOptions.build());
        }

        return builder.build();
    }

    public void close(@Disposes Client client) {
        client.close();
    }
}
