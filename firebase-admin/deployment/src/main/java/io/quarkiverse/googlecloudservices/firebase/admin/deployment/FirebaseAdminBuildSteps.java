package io.quarkiverse.googlecloudservices.firebase.admin.deployment;

import io.quarkiverse.googlecloudservices.firebase.admin.runtime.FirebaseAdminProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class FirebaseAdminBuildSteps {

    private static final String FEATURE = "google-cloud-firebase-admin";

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public AdditionalBeanBuildItem producer() {
        return new AdditionalBeanBuildItem(FirebaseAdminProducer.class);
    }

}
