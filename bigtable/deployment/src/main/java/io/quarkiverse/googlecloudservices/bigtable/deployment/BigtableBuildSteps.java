package io.quarkiverse.googlecloudservices.bigtable.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class BigtableBuildSteps {
    private static final String FEATURE = "google-cloud-bigtable";

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }
}
