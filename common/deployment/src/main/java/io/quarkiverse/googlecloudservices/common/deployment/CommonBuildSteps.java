package io.quarkiverse.googlecloudservices.common.deployment;

import io.quarkiverse.googlecloudservices.common.GcpBootstrapConfiguration;
import io.quarkiverse.googlecloudservices.common.GcpConfigHolder;
import io.quarkiverse.googlecloudservices.common.GcpCredentialProducer;
import io.quarkiverse.googlecloudservices.common.GcpCredentialProviderProducer;
import io.quarkiverse.googlecloudservices.common.GcpCredentialRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.BootstrapConfigSetupCompleteBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;

public class CommonBuildSteps {

    @BuildStep
    public AdditionalBeanBuildItem producer() {
        return new AdditionalBeanBuildItem(GcpCredentialProducer.class,
                GcpConfigHolder.class,
                GcpCredentialProviderProducer.class);
    }

    @BuildStep
    public ExtensionSslNativeSupportBuildItem ssl() {
        return new ExtensionSslNativeSupportBuildItem("google-cloud-common");
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(BootstrapConfigSetupCompleteBuildItem.class)
    public void configure(GcpCredentialRecorder recorder, GcpBootstrapConfiguration bootstrapConfiguration) {
        recorder.configure(bootstrapConfiguration);
    }

    /**
     * Work around for https://github.com/quarkusio/quarkus/issues/25501 until
     * https://github.com/oracle/graal/issues/4543 gets resolved
     *
     * @return
     */
    @BuildStep
    public NativeImageConfigBuildItem nativeImageConfiguration() {
        NativeImageConfigBuildItem.Builder builder = NativeImageConfigBuildItem.builder()
                .addRuntimeReinitializedClass("com.sun.management.internal.PlatformMBeanProviderImpl");
        return builder.build();
    }
}
