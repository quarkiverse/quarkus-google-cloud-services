package io.quarkiverse.googlecloudservices.firestore.runtime;

import java.io.IOException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.threeten.bp.Duration;

import com.google.api.gax.retrying.RetrySettings;
import com.google.auth.Credentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

import io.quarkiverse.googlecloudservices.common.GcpBootstrapConfiguration;
import io.quarkiverse.googlecloudservices.common.GcpConfigHolder;
import io.quarkiverse.googlecloudservices.firestore.runtime.FirestoreConfiguration.RetryConfiguration;

@ApplicationScoped
public class FirestoreProducer {

    @Inject
    Instance<Credentials> googleCredentials;

    @Inject
    GcpConfigHolder gcpConfigHolder;

    @Inject
    FirestoreConfiguration firestoreConfiguration;

    @Inject
    GcpBootstrapConfiguration gcpBootstrapConfiguration;

    @Produces
    @Singleton
    @Default
    public Firestore firestore() throws IOException {
        GcpBootstrapConfiguration gcpConfiguration = gcpConfigHolder.getBootstrapConfig();
        FirestoreOptions.Builder builder = FirestoreOptions.newBuilder()
                .setProjectId(gcpConfiguration.projectId().orElse(null));
        if (useEmulatorCredentials()) {
            builder.setCredentials(new FirestoreOptions.EmulatorCredentials());
            firestoreConfiguration.hostOverride().ifPresent(builder::setEmulatorHost);
        } else {
            builder.setCredentials(googleCredentials.get());
            firestoreConfiguration.hostOverride().ifPresent(builder::setHost);
            firestoreConfiguration.retry().map(this::buildRetrySettings).ifPresent(builder::setRetrySettings);
            firestoreConfiguration.databaseId().ifPresent(builder::setDatabaseId);
        }
        return builder.build().getService();
    }

    private RetrySettings buildRetrySettings(RetryConfiguration retryConfiguration) {
        RetrySettings.Builder retrySettingsBuilder = RetrySettings.newBuilder();
        retryConfiguration.totalTimeout().ifPresent(d -> retrySettingsBuilder.setTotalTimeout(convertDuration(d)));
        retryConfiguration.initialRetryDelay().ifPresent(d -> retrySettingsBuilder.setInitialRetryDelay(convertDuration(d)));
        retryConfiguration.retryDelayMultiplier().ifPresent(retrySettingsBuilder::setRetryDelayMultiplier);
        retryConfiguration.maxRetryDelay().ifPresent(d -> retrySettingsBuilder.setMaxRetryDelay(convertDuration(d)));
        retryConfiguration.maxAttempts().ifPresent(retrySettingsBuilder::setMaxAttempts);
        retryConfiguration.initialRpcTimeout().ifPresent(d -> retrySettingsBuilder.setInitialRpcTimeout(convertDuration(d)));
        retryConfiguration.rpcTimeoutMultiplier().ifPresent(retrySettingsBuilder::setRpcTimeoutMultiplier);
        retryConfiguration.maxRpcTimeout().ifPresent(d -> retrySettingsBuilder.setMaxRpcTimeout(convertDuration(d)));
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

    /**
     * Determine if we need to use emulator credentials. Emulator credentials are used in case the host-override is set,
     * and we don't have accessTokens enabled. The behaviour can be overridden using the useEmulatorCredentials
     * configuration property.
     *
     * @return whether to use the emulator credentials
     */
    private boolean useEmulatorCredentials() {
        return this.firestoreConfiguration.useEmulatorCredentials()
                .orElseGet(this::automaticEmulatorCredentials);
    }

    private boolean automaticEmulatorCredentials() {
        return !this.gcpBootstrapConfiguration.accessTokenEnabled()
                && this.firestoreConfiguration.hostOverride().isPresent()
                && this.firestoreConfiguration.hostOverride().get().contains("localhost");
    }

}
