package io.quarkiverse.googlecloudservices.firebase.admin.deployment;

import java.util.ArrayList;
import java.util.List;

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
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

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

    /**
     * Fix for <a href="https://github.com/quarkiverse/quarkus-google-cloud-services/issues/963">#963</a>.
     */
    @BuildStep
    public NativeImageConfigBuildItem nativeImageConfiguration() {
        return NativeImageConfigBuildItem.builder()
                .addRuntimeInitializedClass("com.google.firebase.internal.ApiClientUtils")
                .addRuntimeInitializedClass("com.google.firebase.internal.ApiClientUtils$TransportInstanceHolder")
                .build();
    }

    /**
     * Fix for <a href="https://github.com/firebase/firebase-admin-java/issues/800">firebase-admin-java#800</a>
     * when running FirebaseAdmin in a native environment.
     */
    @BuildStep
    public List<ReflectiveClassBuildItem> registerReflectiveClasses() {
        List<ReflectiveClassBuildItem> items = new ArrayList<>();
        items.add(ReflectiveClassBuildItem.builder(
                "com.google.firebase.auth.internal.GetAccountInfoResponse",
                "com.google.firebase.auth.internal.GetAccountInfoResponse$User",
                "com.google.firebase.auth.internal.GetAccountInfoResponse$Provider",
                "com.google.api.client.json.GenericJson")
                .constructors(true)
                .fields(true)
                .methods(true)
                .build());
        return items;
    }
}
