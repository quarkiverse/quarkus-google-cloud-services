package io.quarkiverse.googlecloudservices.firebase.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class FirebaseBuildSteps {

    protected static final String FEATURE = "google-cloud-firebase";

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }
}
