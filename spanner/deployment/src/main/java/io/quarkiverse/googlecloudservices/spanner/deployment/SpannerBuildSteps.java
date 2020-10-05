package io.quarkiverse.googlecloudservices.spanner.deployment;

import io.quarkiverse.googlecloudservices.spanner.runtime.SpannerProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public class SpannerBuildSteps {
    private static final String FEATURE = "google-cloud-spanner";

    @BuildStep
    public ReflectiveClassBuildItem registerForReflection() {
        return new ReflectiveClassBuildItem(true, true, "com.google.protobuf.Empty");
    }

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public AdditionalBeanBuildItem producer() {
        return new AdditionalBeanBuildItem(SpannerProducer.class);
    }
}
