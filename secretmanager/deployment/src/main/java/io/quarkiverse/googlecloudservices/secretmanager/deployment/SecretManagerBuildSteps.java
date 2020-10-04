package io.quarkiverse.googlecloudservices.secretmanager.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class SecretManagerBuildSteps {
    private static final String FEATURE = "google-cloud-secretmanager";

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }
}
