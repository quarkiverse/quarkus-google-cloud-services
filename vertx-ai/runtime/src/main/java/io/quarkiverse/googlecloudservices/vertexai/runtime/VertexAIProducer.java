package io.quarkiverse.googlecloudservices.vertexai.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.google.auth.Credentials;
import com.google.cloud.vertexai.VertexAI;

import io.quarkiverse.googlecloudservices.common.GcpBootstrapConfiguration;
import io.quarkiverse.googlecloudservices.common.GcpConfigHolder;

@ApplicationScoped
public class VertexAIProducer {
    @Inject
    Credentials googleCredentials;

    @Inject
    GcpConfigHolder gcpConfigHolder;

    @Inject
    VertxAIConfiguration vertxAIConfiguration;

    @Produces
    @Singleton
    @Default
    public VertexAI vertexAI() {
        GcpBootstrapConfiguration gcpConfiguration = gcpConfigHolder.getBootstrapConfig();

        var builder = new VertexAI.Builder()
                .setCredentials(googleCredentials);

        gcpConfiguration.projectId().ifPresent(builder::setProjectId);
        vertxAIConfiguration.location().ifPresent(builder::setLocation);
        vertxAIConfiguration.apiEndpoint().ifPresent(builder::setApiEndpoint);

        return builder.build();
    }

    public void close(@Disposes VertexAI vertexAI) {
        vertexAI.close();
    }
}
