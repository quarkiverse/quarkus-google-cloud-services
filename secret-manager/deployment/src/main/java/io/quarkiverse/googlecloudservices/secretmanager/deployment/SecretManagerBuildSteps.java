package io.quarkiverse.googlecloudservices.secretmanager.deployment;

import io.quarkiverse.googlecloudservices.secretmanager.runtime.SecretManagerProducer;
import io.quarkiverse.googlecloudservices.secretmanager.runtime.config.SecretManagerClientDestroyer;
import io.quarkiverse.googlecloudservices.secretmanager.runtime.config.SecretManagerConfigBuilder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;

public class SecretManagerBuildSteps {
    private static final String FEATURE = "google-cloud-secret-manager";

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public void additionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem(SecretManagerProducer.class));
        additionalBeans.produce(new AdditionalBeanBuildItem(SecretManagerClientDestroyer.class));
    }

    @BuildStep
    public RunTimeConfigBuilderBuildItem secretManagerConfig() {
        return new RunTimeConfigBuilderBuildItem(SecretManagerConfigBuilder.class);
    }
}
