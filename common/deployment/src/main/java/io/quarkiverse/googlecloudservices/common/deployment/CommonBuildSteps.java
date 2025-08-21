package io.quarkiverse.googlecloudservices.common.deployment;

import io.quarkiverse.googlecloudservices.common.GcpConfigHolder;
import io.quarkiverse.googlecloudservices.common.GcpCredentialProducer;
import io.quarkiverse.googlecloudservices.common.GcpCredentialProviderProducer;
import io.quarkiverse.googlecloudservices.common.GcpDefaultsConfigBuilder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;
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
    public RunTimeConfigBuilderBuildItem defaultsConfig() {
        return new RunTimeConfigBuilderBuildItem(GcpDefaultsConfigBuilder.class);
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
                .addRuntimeReinitializedClass("com.sun.management.internal.PlatformMBeanProviderImpl")
                // Required due to sun.misc.Unsafe usage in static initializers
                .addRuntimeReinitializedClass("com.google.common.cache.Striped64")
                // Required due to initializing a java.util.Random
                .addRuntimeReinitializedClass("io.opentelemetry.sdk.internal.AndroidFriendlyRandomHolder")
                .addRuntimeInitializedClass("io.grpc.netty.shaded.io.netty.util.internal.logging.Log4JLoggerFactory");
        return builder.build();
    }
}
