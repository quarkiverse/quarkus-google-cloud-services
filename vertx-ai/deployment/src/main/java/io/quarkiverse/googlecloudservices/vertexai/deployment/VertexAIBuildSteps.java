package io.quarkiverse.googlecloudservices.vertexai.deployment;

import io.quarkiverse.googlecloudservices.vertexai.runtime.VertexAIProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class VertexAIBuildSteps {
    private static final String FEATURE = "google-cloud-vertex-ai";

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public AdditionalBeanBuildItem additionalBean() {
        return new AdditionalBeanBuildItem(VertexAIProducer.class);
    }
}
