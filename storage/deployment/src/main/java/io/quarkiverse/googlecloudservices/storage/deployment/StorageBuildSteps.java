package io.quarkiverse.googlecloudservices.storage.deployment;

import io.quarkiverse.googlecloudservices.storage.runtime.StorageProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class StorageBuildSteps {
    private static final String FEATURE = "google-cloud-storage";

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public AdditionalBeanBuildItem producer() {
        return new AdditionalBeanBuildItem(StorageProducer.class);
    }
}
