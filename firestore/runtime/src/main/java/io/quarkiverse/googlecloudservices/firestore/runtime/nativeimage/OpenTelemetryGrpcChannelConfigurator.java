package io.quarkiverse.googlecloudservices.firestore.runtime.nativeimage;

import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry;

@TargetClass(className = "com.google.cloud.firestore.telemetry.EnabledTraceUtil$OpenTelemetryGrpcChannelConfigurator", onlyWith = OpenTelemetryGrpcChannelConfigurator.OnlyIfInClassPath.class)
public final class OpenTelemetryGrpcChannelConfigurator {
    @Substitute
    public ManagedChannelBuilder apply(ManagedChannelBuilder managedChannelBuilder) {
        GrpcTelemetry grpcTelemetry = GrpcTelemetry.create(GlobalOpenTelemetry.get());
        return managedChannelBuilder.intercept(grpcTelemetry.createClientInterceptor());
    }

    public static class OnlyIfInClassPath implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName("com.google.cloud.firestore.telemetry.EnabledTraceUtil$OpenTelemetryGrpcChannelConfigurator",
                        false,
                        Thread.currentThread().getContextClassLoader());
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }
}
