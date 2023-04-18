package io.quarkiverse.googlecloudservices.common.grpc.runtime.graal;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import org.threeten.bp.Duration;

import com.google.api.core.ApiFunction;
import com.google.api.gax.grpc.ChannelPrimer;
import com.google.api.gax.grpc.GrpcHeaderInterceptor;
import com.google.api.gax.grpc.GrpcInterceptorProvider;
import com.google.api.gax.rpc.HeaderProvider;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;

@TargetClass(className = "com.google.api.gax.grpc.InstantiatingGrpcChannelProvider")
final class Target_com_google_api_gax_grpc_InstantiatingGrpcChannelProvider {

    @Alias
    private Executor executor;
    @Alias
    private HeaderProvider headerProvider;
    @Alias
    private GrpcInterceptorProvider interceptorProvider;
    @Alias
    private String endpoint;
    @Alias
    private Integer maxInboundMessageSize;
    @Alias
    private Integer maxInboundMetadataSize;
    @Alias
    private Duration keepAliveTime;
    @Alias
    private Duration keepAliveTimeout;
    @Alias
    private Boolean keepAliveWithoutCalls;
    @Alias
    private ChannelPrimer channelPrimer;
    @Alias
    private ApiFunction<ManagedChannelBuilder, ManagedChannelBuilder> channelConfigurator;

    @Substitute
    private ManagedChannel createSingleChannel() throws IOException {
        GrpcHeaderInterceptor headerInterceptor = new GrpcHeaderInterceptor(this.headerProvider.getHeaders());
        Target_com_google_api_gax_grpc_GrpcMetadataHandlerInterceptor metadataHandlerInterceptor = new Target_com_google_api_gax_grpc_GrpcMetadataHandlerInterceptor();
        int colon = this.endpoint.lastIndexOf(58);
        if (colon < 0) {
            throw new IllegalStateException("invalid endpoint - should have been validated: " + this.endpoint);
        } else {
            int port = Integer.parseInt(this.endpoint.substring(colon + 1));
            String serviceAddress = this.endpoint.substring(0, colon);
            //            Object builder;
            //            if (this.isDirectPathEnabled(serviceAddress) && this.credentials instanceof ComputeEngineCredentials) {
            //                builder = ComputeEngineChannelBuilder.forAddress(serviceAddress, port);
            //                ((ManagedChannelBuilder)builder).keepAliveTime(3600L, TimeUnit.SECONDS);
            //                ((ManagedChannelBuilder)builder).keepAliveTimeout(20L, TimeUnit.SECONDS);
            //                ImmutableMap<String, Object> pickFirstStrategy = ImmutableMap.of("pick_first", ImmutableMap.of());
            //                ImmutableMap<String, Object> childPolicy = ImmutableMap.of("childPolicy", ImmutableList.of(pickFirstStrategy));
            //                ImmutableMap<String, Object> grpcLbPolicy = ImmutableMap.of("grpclb", childPolicy);
            //                ImmutableMap<String, Object> loadBalancingConfig = ImmutableMap.of("loadBalancingConfig", ImmutableList.of(grpcLbPolicy));
            //                ((ManagedChannelBuilder)builder).defaultServiceConfig(loadBalancingConfig);
            //            } else {
            //                builder = ManagedChannelBuilder.forAddress(serviceAddress, port);
            //            }

            ManagedChannelBuilder builder = ManagedChannelBuilder.forAddress(serviceAddress, port);
            builder = ((ManagedChannelBuilder) builder).disableServiceConfigLookUp()
                    .intercept(new ClientInterceptor[] { new Target_com_google_api_gax_grpc_GrpcChannelUUIDInterceptor() })
                    .intercept(new ClientInterceptor[] { headerInterceptor })
                    .intercept(new ClientInterceptor[] { metadataHandlerInterceptor })
                    .userAgent(headerInterceptor.getUserAgentHeader()).executor(this.executor);
            if (this.maxInboundMetadataSize != null) {
                builder.maxInboundMetadataSize(this.maxInboundMetadataSize);
            }

            if (this.maxInboundMessageSize != null) {
                builder.maxInboundMessageSize(this.maxInboundMessageSize);
            }

            if (this.keepAliveTime != null) {
                builder.keepAliveTime(this.keepAliveTime.toMillis(), TimeUnit.MILLISECONDS);
            }

            if (this.keepAliveTimeout != null) {
                builder.keepAliveTimeout(this.keepAliveTimeout.toMillis(), TimeUnit.MILLISECONDS);
            }

            if (this.keepAliveWithoutCalls != null) {
                builder.keepAliveWithoutCalls(this.keepAliveWithoutCalls);
            }

            if (this.interceptorProvider != null) {
                builder.intercept(this.interceptorProvider.getInterceptors());
            }

            if (this.channelConfigurator != null) {
                builder = (ManagedChannelBuilder) this.channelConfigurator.apply(builder);
            }

            ManagedChannel managedChannel = builder.build();
            if (this.channelPrimer != null) {
                this.channelPrimer.primeChannel(managedChannel);
            }

            return managedChannel;
        }
    }
}

@TargetClass(className = "com.google.api.gax.grpc.GrpcMetadataHandlerInterceptor")
final class Target_com_google_api_gax_grpc_GrpcMetadataHandlerInterceptor implements ClientInterceptor {

    @Alias()
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, final CallOptions callOptions, Channel next) {
        throw new UnsupportedOperationException("Alias should not be called");
    }
}

@TargetClass(className = "com.google.api.gax.grpc.GrpcChannelUUIDInterceptor")
final class Target_com_google_api_gax_grpc_GrpcChannelUUIDInterceptor implements ClientInterceptor {

    @Alias
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel channel) {
        throw new UnsupportedOperationException("Alias should not be called");
    }
}

@TargetClass(className = "io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLoggerFactory")
final class Target_io_grpc_netty_shaded_io_netty_util_internal_logging_InternalLoggerFactory {

    @Substitute
    static InternalLogger getInstance(Class<?> clazz) {
        return new InternalLogger() {
            @Override
            public String name() {
                return "noop";
            }

            @Override
            public boolean isTraceEnabled() {
                return false;
            }

            @Override
            public void trace(String s) {

            }

            @Override
            public void trace(String s, Object o) {

            }

            @Override
            public void trace(String s, Object o, Object o1) {

            }

            @Override
            public void trace(String s, Object... objects) {

            }

            @Override
            public void trace(String s, Throwable throwable) {

            }

            @Override
            public void trace(Throwable throwable) {

            }

            @Override
            public boolean isDebugEnabled() {
                return false;
            }

            @Override
            public void debug(String s) {

            }

            @Override
            public void debug(String s, Object o) {

            }

            @Override
            public void debug(String s, Object o, Object o1) {

            }

            @Override
            public void debug(String s, Object... objects) {

            }

            @Override
            public void debug(String s, Throwable throwable) {

            }

            @Override
            public void debug(Throwable throwable) {

            }

            @Override
            public boolean isInfoEnabled() {
                return false;
            }

            @Override
            public void info(String s) {

            }

            @Override
            public void info(String s, Object o) {

            }

            @Override
            public void info(String s, Object o, Object o1) {

            }

            @Override
            public void info(String s, Object... objects) {

            }

            @Override
            public void info(String s, Throwable throwable) {

            }

            @Override
            public void info(Throwable throwable) {

            }

            @Override
            public boolean isWarnEnabled() {
                return false;
            }

            @Override
            public void warn(String s) {

            }

            @Override
            public void warn(String s, Object o) {

            }

            @Override
            public void warn(String s, Object... objects) {

            }

            @Override
            public void warn(String s, Object o, Object o1) {

            }

            @Override
            public void warn(String s, Throwable throwable) {

            }

            @Override
            public void warn(Throwable throwable) {

            }

            @Override
            public boolean isErrorEnabled() {
                return false;
            }

            @Override
            public void error(String s) {

            }

            @Override
            public void error(String s, Object o) {

            }

            @Override
            public void error(String s, Object o, Object o1) {

            }

            @Override
            public void error(String s, Object... objects) {

            }

            @Override
            public void error(String s, Throwable throwable) {

            }

            @Override
            public void error(Throwable throwable) {

            }

            @Override
            public boolean isEnabled(InternalLogLevel internalLogLevel) {
                return false;
            }

            @Override
            public void log(InternalLogLevel internalLogLevel, String s) {

            }

            @Override
            public void log(InternalLogLevel internalLogLevel, String s, Object o) {

            }

            @Override
            public void log(InternalLogLevel internalLogLevel, String s, Object o, Object o1) {

            }

            @Override
            public void log(InternalLogLevel internalLogLevel, String s, Object... objects) {

            }

            @Override
            public void log(InternalLogLevel internalLogLevel, String s, Throwable throwable) {

            }

            @Override
            public void log(InternalLogLevel internalLogLevel, Throwable throwable) {

            }
        };
    }
}

class GoogleApiGaxGrpcSubstitutions {
}
