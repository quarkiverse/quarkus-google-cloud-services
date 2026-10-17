package io.quarkiverse.googlecloudservices.sheets.deployment;

import io.quarkiverse.googlecloudservices.sheets.runtime.SheetsProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class SheetsBuildSteps {
    private static final String FEATURE = "google-cloud-sheets";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem producer() {
        return new AdditionalBeanBuildItem(SheetsProducer.class);
    }
}
