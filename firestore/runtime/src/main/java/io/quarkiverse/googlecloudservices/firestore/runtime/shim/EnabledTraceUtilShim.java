package io.quarkiverse.googlecloudservices.firestore.runtime.shim;

import com.google.api.core.ApiFunction;
import com.google.cloud.firestore.telemetry.EnabledTraceUtil;

import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry;
import io.quarkiverse.shim.Shim;
import io.quarkiverse.shim.ShimFields;
import io.quarkiverse.shim.ShimReplace;

/**
 * Patches {@link EnabledTraceUtil#getChannelConfigurator()} so the gRPC channel configurator it
 * returns wires up OpenTelemetry gRPC instrumentation via this extension's own OpenTelemetry
 * dependency, rather than whatever conflicting OpenTelemetry libraries may otherwise end up on
 * the classpath.
 */
@Shim(EnabledTraceUtil.class)
public class EnabledTraceUtilShim {

    @ShimReplace(method = "getChannelConfigurator")
    public static ApiFunction<ManagedChannelBuilder, ManagedChannelBuilder> getChannelConfigurator(EnabledTraceUtil self) {
        var openTelemetry = ShimFields.<OpenTelemetry> get(self, "openTelemetry");
        return new PatchedGrpcConfigurator(openTelemetry);
    }

    public static class PatchedGrpcConfigurator implements ApiFunction<ManagedChannelBuilder, ManagedChannelBuilder> {

        private final OpenTelemetry openTelemetry;

        public PatchedGrpcConfigurator(OpenTelemetry openTelemetry) {
            this.openTelemetry = openTelemetry;
        }

        @Override
        public ManagedChannelBuilder apply(ManagedChannelBuilder builder) {
            var grpcTelemetry = GrpcTelemetry.create(this.openTelemetry);
            return builder.intercept(grpcTelemetry.createClientInterceptor());
        }
    }
}
