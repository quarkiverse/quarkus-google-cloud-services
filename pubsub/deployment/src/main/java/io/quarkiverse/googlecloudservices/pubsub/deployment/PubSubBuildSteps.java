package io.quarkiverse.googlecloudservices.pubsub.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class PubSubBuildSteps {
    public static final String FEATURE = "google-cloud-pubsub";

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }
}
