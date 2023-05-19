package io.quarkiverse.googlecloudservices.firebase.admin.deployment;

import io.quarkiverse.googlecloudservices.firebase.admin.deployment.authentication.FirebaseAuthConfiguration;
import io.quarkiverse.googlecloudservices.firebase.admin.runtime.FirebaseAdminProducer;
import io.quarkiverse.googlecloudservices.firebase.admin.runtime.authentication.http.DefaultFirebaseIdentityProvider;
import io.quarkiverse.googlecloudservices.firebase.admin.runtime.authentication.http.FirebaseSecurityAuthMechanism;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
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

    @BuildStep
    public void setupFirebaseAuth(BuildProducer<AdditionalBeanBuildItem> additionalBeans, FirebaseAuthConfiguration config) {
        if (!config.enabled) {
            return;
        }

        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder().setUnremovable();

        builder.addBeanClasses(DefaultFirebaseIdentityProvider.class, FirebaseSecurityAuthMechanism.class);
        additionalBeans.produce(builder.build());
    }
}
