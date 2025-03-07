package io.quarkiverse.googlecloudservices.spanner.deployment;

import io.quarkiverse.googlecloudservices.spanner.runtime.SpannerProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class SpannerBuildSteps {
    protected static final String FEATURE = "google-cloud-spanner";

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public AdditionalBeanBuildItem producer() {
        return new AdditionalBeanBuildItem(SpannerProducer.class);
    }
}
