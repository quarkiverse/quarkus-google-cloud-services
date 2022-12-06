package io.quarkiverse.googlecloudservices.secretmanager.deployment;

import io.quarkiverse.googlecloudservices.common.GcpBootstrapConfiguration;
import io.quarkiverse.googlecloudservices.common.deployment.GcpConfigurationDoneBuildItem;
import io.quarkiverse.googlecloudservices.secretmanager.runtime.SecretManagerProducer;
import io.quarkiverse.googlecloudservices.secretmanager.runtime.SecretManagerRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationSourceValueBuildItem;

public class SecretManagerBuildSteps {
    private static final String FEATURE = "google-cloud-secret-manager";

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public AdditionalBeanBuildItem producer() {
        return new AdditionalBeanBuildItem(SecretManagerProducer.class);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(GcpConfigurationDoneBuildItem.class)
    public RunTimeConfigurationSourceValueBuildItem configure(SecretManagerRecorder recorder,
            GcpBootstrapConfiguration bootstrapConfiguration) {
        return new RunTimeConfigurationSourceValueBuildItem(
                recorder.configSources(bootstrapConfiguration));
    }
}
