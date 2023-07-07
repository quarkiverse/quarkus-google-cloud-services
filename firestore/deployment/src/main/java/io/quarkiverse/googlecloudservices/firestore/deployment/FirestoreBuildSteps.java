package io.quarkiverse.googlecloudservices.firestore.deployment;

import io.quarkiverse.googlecloudservices.firestore.runtime.FirestoreProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class FirestoreBuildSteps {
    protected static final String FEATURE = "google-cloud-firestore";

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public AdditionalBeanBuildItem producer() {
        return new AdditionalBeanBuildItem(FirestoreProducer.class);
    }
}
