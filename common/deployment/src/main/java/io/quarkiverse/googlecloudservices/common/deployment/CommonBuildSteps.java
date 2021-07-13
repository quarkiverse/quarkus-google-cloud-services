package io.quarkiverse.googlecloudservices.common.deployment;

import com.google.cloud.ServiceOptions;

import io.quarkiverse.googlecloudservices.common.GcpBootstrapConfiguration;
import io.quarkiverse.googlecloudservices.common.GcpConfigHolder;
import io.quarkiverse.googlecloudservices.common.GcpCredentialProducer;
import io.quarkiverse.googlecloudservices.common.GcpCredentialRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;

public class CommonBuildSteps {

    @BuildStep
    public AdditionalBeanBuildItem producer() {
        return new AdditionalBeanBuildItem(GcpCredentialProducer.class, GcpConfigHolder.class);
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

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void configure(GcpCredentialRecorder recorder, GcpBootstrapConfiguration bootstrapConfiguration) {
        recorder.configure(bootstrapConfiguration);
    }
}
