package io.quarkiverse.googlecloudservices.firebase.admin.deployment;

import io.quarkiverse.googlecloudservices.firebase.admin.deployment.authentication.FirebaseAuthConfiguration;
import io.quarkiverse.googlecloudservices.firebase.admin.runtime.FirebaseAdminProducer;
import io.quarkiverse.googlecloudservices.firebase.admin.runtime.FirebaseSessionCookieManager;
import io.quarkiverse.googlecloudservices.firebase.admin.runtime.authentication.FirebaseSessionCookieConfiguration;
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
    public void setupFirebaseAuth(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            FirebaseAuthConfiguration config,
            FirebaseSessionCookieConfiguration sessionCookieConfig) {
        if (!config.enabled()) {
            return;
        }

        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder().setUnremovable();

        builder.addBeanClasses(DefaultFirebaseIdentityProvider.class, FirebaseSecurityAuthMechanism.class);

        if (sessionCookieConfig.enabled()) {
            builder.addBeanClasses(FirebaseSessionCookieManager.class);
        }

        additionalBeans.produce(builder.build());
    }
}
