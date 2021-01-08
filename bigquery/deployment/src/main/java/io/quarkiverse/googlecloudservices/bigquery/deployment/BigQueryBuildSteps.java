package io.quarkiverse.googlecloudservices.bigquery.deployment;

import io.quarkiverse.googlecloudservices.bigquery.runtime.BigQueryProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

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
}
