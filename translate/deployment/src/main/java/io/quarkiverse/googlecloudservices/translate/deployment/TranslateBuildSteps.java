package io.quarkiverse.googlecloudservices.translate.deployment;

import io.quarkiverse.googlecloudservices.translate.runtime.TranslateProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class TranslateBuildSteps {
    private static final String FEATURE = "google-cloud-translate";

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public AdditionalBeanBuildItem producer() {
        return new AdditionalBeanBuildItem(TranslateProducer.class);
    }
}
