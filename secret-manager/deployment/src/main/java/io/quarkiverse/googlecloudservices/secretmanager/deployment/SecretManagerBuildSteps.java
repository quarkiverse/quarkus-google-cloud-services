package io.quarkiverse.googlecloudservices.secretmanager.deployment;

import io.quarkiverse.googlecloudservices.secretmanager.runtime.SecretManagerProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class SecretManagerBuildSteps {
    private static final String FEATURE = "google-cloud-secret-manager";

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public AdditionalBeanBuildItem producer() {
        return new AdditionalBeanBuildItem(SecretManagerProducer.class);
    }
}
