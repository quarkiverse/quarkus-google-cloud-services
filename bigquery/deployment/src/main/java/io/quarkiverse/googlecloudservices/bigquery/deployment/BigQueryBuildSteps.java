package io.quarkiverse.googlecloudservices.bigquery.deployment;

import java.util.List;

import io.quarkiverse.googlecloudservices.bigquery.runtime.BigQueryProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

public class BigQueryBuildSteps {
    private static final String FEATURE = "google-cloud-bigquery";

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public AdditionalBeanBuildItem producer() {
        return new AdditionalBeanBuildItem(BigQueryProducer.class);
    }

    @BuildStep
    public List<RuntimeInitializedClassBuildItem> runtimeInitializedClass() {
        return List.of(
                new RuntimeInitializedClassBuildItem("org.apache.arrow.memory.BaseAllocator"),
                new RuntimeInitializedClassBuildItem("org.apache.arrow.memory.unsafe.DefaultAllocationManagerFactory"),
                new RuntimeInitializedClassBuildItem("org.apache.arrow.memory.netty.NettyAllocationManager"));
    }
}
