package io.quarkiverse.googlecloudservices.firebase.database.deployment;

import io.quarkiverse.googlecloudservices.firebase.database.FirebaseDatabaseProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class FirebaseDatabaseBuildSteps {

    private static final String FEATURE = "google-cloud-firebase-realtime-database";

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public AdditionalBeanBuildItem producer() {
        return new AdditionalBeanBuildItem(FirebaseDatabaseProducer.class);
    }

}
