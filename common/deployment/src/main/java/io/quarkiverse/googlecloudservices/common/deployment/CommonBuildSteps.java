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
                .addRuntimeInitializedClass("com.sun.management.internal.PlatformMBeanProviderImpl")
                // Required due to initializing a java.util.Random
                .addRuntimeInitializedClass("io.opentelemetry.sdk.internal.AndroidFriendlyRandomHolder")
                // Required for Netty HTTP
                .addRuntimeInitializedClass("io.netty.handler.codec.compression.ZstdConstants")
                .addRuntimeInitializedClass("io.netty.handler.codec.compression.BrotliOptions")
                .addRuntimeInitializedClass("io.vertx.core.buffer.impl.VertxByteBufAllocator");
        return builder.build();
    }
}
