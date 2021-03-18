package io.quarkiverse.googlecloudservices.common.deployment;

import com.google.cloud.ServiceOptions;

import io.quarkiverse.googlecloudservices.common.GcpCredentialProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;

import java.io.IOException;

public class CommonBuildSteps {

    @BuildStep
    public AdditionalBeanBuildItem producer() {
        return new AdditionalBeanBuildItem(GcpCredentialProducer.class);
    }

    @BuildStep
    public ExtensionSslNativeSupportBuildItem ssl() {
        return new ExtensionSslNativeSupportBuildItem("google-cloud-common");
    }

    @BuildStep
    public RunTimeConfigurationDefaultBuildItem defaultProjectId() {
        String defaultObject = ServiceOptions.getDefaultProjectId();
        if (defaultObject != null) {
            return new RunTimeConfigurationDefaultBuildItem("quarkus.google.cloud.project-id",
                    ServiceOptions.getDefaultProjectId());
        }
        return null;
    }
}
