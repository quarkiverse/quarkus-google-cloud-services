package io.quarkiverse.googlecloudservices.firestore.runtime;

import java.io.IOException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.threeten.bp.Duration;

import com.google.api.gax.retrying.RetrySettings;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

import io.quarkiverse.googlecloudservices.common.GcpBootstrapConfiguration;
import io.quarkiverse.googlecloudservices.common.GcpConfigHolder;
import io.quarkiverse.googlecloudservices.firestore.runtime.FirestoreConfiguration.RetryConfiguration;

@ApplicationScoped
public class FirestoreProducer {

    @Inject
    GoogleCredentials googleCredentials;

    @Inject
    GcpConfigHolder gcpConfigHolder;

    @Inject
    FirestoreConfiguration firestoreConfiguration;

    @ConfigProperty(name = "quarkus.google.cloud.firestore.devservice.enabled", defaultValue = "false")
    boolean devServiceEnabled;

    @ConfigProperty(name = "quarkus.google.cloud.firestore.emulator-host")
    String emulatorHost;

    @Produces
    @Singleton
    @Default
    public Firestore firestore() throws IOException {
        GcpBootstrapConfiguration gcpConfiguration = gcpConfigHolder.getBootstrapConfig();
        FirestoreOptions.Builder builder = FirestoreOptions.newBuilder()
                .setCredentials(googleCredentials)
                .setProjectId(gcpConfiguration.projectId.orElse(null));

        if (devServiceEnabled) {
            builder.setEmulatorHost(emulatorHost);
        }

        firestoreConfiguration.hostOverride.ifPresent(builder::setHost);
        firestoreConfiguration.retry.ifPresent(retry -> builder.setRetrySettings(buildRetrySettings(retry)));
        return builder.build().getService();
    }

    private RetrySettings buildRetrySettings(RetryConfiguration retryConfiguration) {
        RetrySettings.Builder retrySettingsBuilder = RetrySettings.newBuilder();
        retryConfiguration.totalTimeout.ifPresent(d -> retrySettingsBuilder.setTotalTimeout(convertDuration(d)));
        retryConfiguration.initialRetryDelay.ifPresent(d -> retrySettingsBuilder.setInitialRetryDelay(convertDuration(d)));
        retryConfiguration.retryDelayMultiplier.ifPresent(retrySettingsBuilder::setRetryDelayMultiplier);
        retryConfiguration.maxRetryDelay.ifPresent(d -> retrySettingsBuilder.setMaxRetryDelay(convertDuration(d)));
        retryConfiguration.maxAttempts.ifPresent(retrySettingsBuilder::setMaxAttempts);
        retryConfiguration.initialRpcTimeout.ifPresent(d -> retrySettingsBuilder.setInitialRpcTimeout(convertDuration(d)));
        retryConfiguration.rpcTimeoutMultiplier.ifPresent(retrySettingsBuilder::setRpcTimeoutMultiplier);
        retryConfiguration.maxRpcTimeout.ifPresent(d -> retrySettingsBuilder.setMaxRpcTimeout(convertDuration(d)));
        return retrySettingsBuilder.build();
    }

    /**
     * Converts java.time.Duration to org.threeten.bp.Duration used by Firestore
     *
     * @param duration deserialized from configuration
     * @return transformed Duration that is accepted by Firestore
     */
    private Duration convertDuration(java.time.Duration duration) {
        return Duration.ofMillis(duration.toMillis());
    }

}
