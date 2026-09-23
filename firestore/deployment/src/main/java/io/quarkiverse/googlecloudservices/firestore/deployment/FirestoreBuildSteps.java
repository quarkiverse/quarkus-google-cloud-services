package io.quarkiverse.googlecloudservices.firestore.deployment;

import io.quarkiverse.googlecloudservices.firestore.runtime.FirestoreProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;

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

    /**
     * google-cloud-firestore is a plain third-party jar, not a Quarkus extension, so Quarkus never
     * Jandex-indexes it on its own. EnabledTraceUtilShim's {@code @Shim} targets
     * {@code EnabledTraceUtil} in that jar
     */
    @BuildStep
    public IndexDependencyBuildItem indexGoogleCloudFirestoreForShim() {
        return new IndexDependencyBuildItem("com.google.cloud", "google-cloud-firestore");
    }

    /**
     * Make sure that the runtime jar of this extension is indexed, so that {@code FirestoreProducer} can be found by
     * {@code @Shim}
     */
    @BuildStep
    public IndexDependencyBuildItem indexOwnRuntimeJarForShim() {
        return new IndexDependencyBuildItem("io.quarkiverse.googlecloudservices", "quarkus-google-cloud-firestore");
    }
}
