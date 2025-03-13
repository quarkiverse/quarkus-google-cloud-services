package io.quarkiverse.googlecloudservices.storage.runtime;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.google.api.gax.retrying.RetrySettings;
import com.google.auth.Credentials;
import com.google.cloud.TransportOptions;
import com.google.cloud.grpc.GrpcTransportOptions;
import com.google.cloud.http.HttpTransportOptions;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import io.quarkiverse.googlecloudservices.common.GcpBootstrapConfiguration;
import io.quarkiverse.googlecloudservices.common.GcpConfigHolder;

@ApplicationScoped
public class StorageProducer {

    @Inject
    Credentials googleCredentials;

    @Inject
    GcpConfigHolder gcpConfigHolder;

    @Inject
    StorageConfiguration storageConfiguration;

    @Produces
    @Singleton
    @Default
    public Storage storage() throws IOException {
        GcpBootstrapConfiguration gcpConfiguration = gcpConfigHolder.getBootstrapConfig();
        StorageOptions.Builder builder = StorageOptions.newBuilder()
                .setCredentials(googleCredentials);

        gcpConfiguration.projectId().ifPresent(builder::setProjectId);
        storageConfiguration.hostOverride().ifPresent(builder::setHost);

        builder.setTransportOptions(transportOptions());

        retrySettings().ifPresent(builder::setRetrySettings);
        return builder.build().getService();
    }

    private TransportOptions transportOptions() {
        return storageConfiguration.transport().useGrpc() ? grpcTransportOption()
                : httpTransportOptions();
    }

    private TransportOptions httpTransportOptions() {
        var builder = HttpTransportOptions.newBuilder();

        storageConfiguration.transport().httpConnectTimeout().ifPresent(builder::setConnectTimeout);
        storageConfiguration.transport().httpReadTimeout().ifPresent(builder::setReadTimeout);

        return builder.build();
    }

    private TransportOptions grpcTransportOption() {
        var builder = GrpcTransportOptions.newBuilder();
        return builder.build();
    }

    private Optional<RetrySettings> retrySettings() {
        var config = storageConfiguration.retry();

        return config.initialRetryDelayMillis()
                .or(config::initialRpcTimeoutMillis)
                .or(config::logicalTimeoutMillis)
                .or(config::maxAttempts)
                .or(config::maxRpcTimeoutMillis)
                .or(config::maxRetryDelayMillis)
                .or(config::totalTimeoutMillis)
                .map(o -> {
                    var builder = RetrySettings.newBuilder();

                    config.initialRetryDelayMillis().map(this::toDuration).ifPresent(builder::setInitialRetryDelayDuration);
                    config.maxRetryDelayMillis().map(this::toDuration).ifPresent(builder::setMaxRetryDelayDuration);
                    config.initialRpcTimeoutMillis().map(this::toDuration).ifPresent(builder::setInitialRpcTimeoutDuration);
                    config.maxRpcTimeoutMillis().map(this::toDuration).ifPresent(builder::setMaxRpcTimeoutDuration);
                    config.logicalTimeoutMillis().map(this::toDuration).ifPresent(builder::setLogicalTimeout);
                    config.totalTimeoutMillis().map(this::toDuration).ifPresent(builder::setTotalTimeoutDuration);
                    config.maxAttempts().ifPresent(builder::setMaxAttempts);

                    return builder.build();
                });
    }

    private Duration toDuration(Integer integer) {
        return Duration.ofMillis(integer);
    }

    public void close(@Disposes Storage storage) throws Exception {
        storage.close();
    }
}
