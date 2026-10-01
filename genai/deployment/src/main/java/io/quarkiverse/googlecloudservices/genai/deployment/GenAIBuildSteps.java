package io.quarkiverse.googlecloudservices.genai.deployment;

import io.quarkiverse.googlecloudservices.genai.runtime.GenAIProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

public class GenAIBuildSteps {
    private static final String FEATURE = "google-cloud-genai";

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public AdditionalBeanBuildItem additionalBean() {
        return new AdditionalBeanBuildItem(GenAIProducer.class);
    }

    @BuildStep
    public RuntimeInitializedClassBuildItem localTokenizerLoader() {
        // holds a static OkHttpClient that must not end up in the image heap
        return new RuntimeInitializedClassBuildItem("com.google.genai.LocalTokenizerLoader");
    }
}
