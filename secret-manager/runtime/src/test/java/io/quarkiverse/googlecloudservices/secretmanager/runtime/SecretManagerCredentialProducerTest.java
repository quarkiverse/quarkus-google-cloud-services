package io.quarkiverse.googlecloudservices.secretmanager.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.auth.Credentials;
import com.google.cloud.NoCredentials;

import io.quarkiverse.googlecloudservices.common.GcpBootstrapConfiguration;
import io.quarkiverse.googlecloudservices.common.GcpConfigHolder;
import io.quarkiverse.googlecloudservices.common.GcpCredentialsFactory;

class SecretManagerCredentialProducerTest {

    private final RecordingCredentialsFactory credentialsFactory = new RecordingCredentialsFactory();
    private final GcpConfigHolder configHolder = mock(GcpConfigHolder.class);
    private final SecretManagerCredentialProducer producer = new SecretManagerCredentialProducer();

    @BeforeEach
    void setUp() {
        producer.credentialsFactory = credentialsFactory;
        producer.gcpConfigHolder = configHolder;
    }

    @Test
    void inheritsGlobalAccessTokenConfiguration() throws Exception {
        givenGlobalConfiguration(Optional.empty(), Optional.empty(), true);
        producer.configuration = configuration(Optional.empty());

        producer.secretManagerCredentials();

        assertThat(credentialsFactory.accessTokenEnabled).isTrue();
    }

    @Test
    void disablesAccessTokenForSecretManagerOnly() throws Exception {
        givenGlobalConfiguration(Optional.empty(), Optional.empty(), true);
        producer.configuration = configuration(Optional.of(false));

        producer.secretManagerCredentials();

        assertThat(credentialsFactory.accessTokenEnabled).isFalse();
    }

    @Test
    void enablesAccessTokenForSecretManagerOnly() throws Exception {
        givenGlobalConfiguration(Optional.empty(), Optional.empty(), false);
        producer.configuration = configuration(Optional.of(true));

        producer.secretManagerCredentials();

        assertThat(credentialsFactory.accessTokenEnabled).isTrue();
    }

    @Test
    void forwardsGlobalServiceAccountConfiguration() throws Exception {
        givenGlobalConfiguration(Optional.of("/path/service-account.json"), Optional.of("encoded-key"), false);
        producer.configuration = configuration(Optional.empty());

        producer.secretManagerCredentials();

        assertThat(credentialsFactory.serviceAccountLocation).contains("/path/service-account.json");
        assertThat(credentialsFactory.serviceAccountEncodedKey).contains("encoded-key");
    }

    private void givenGlobalConfiguration(Optional<String> serviceAccountLocation,
            Optional<String> serviceAccountEncodedKey, boolean accessTokenEnabled) {
        GcpBootstrapConfiguration globalConfiguration = mock(GcpBootstrapConfiguration.class);
        when(globalConfiguration.serviceAccountLocation()).thenReturn(serviceAccountLocation);
        when(globalConfiguration.serviceAccountEncodedKey()).thenReturn(serviceAccountEncodedKey);
        when(globalConfiguration.accessTokenEnabled()).thenReturn(accessTokenEnabled);
        when(configHolder.getBootstrapConfig()).thenReturn(globalConfiguration);
    }

    private static SecretManagerConfiguration configuration(Optional<Boolean> accessTokenEnabled) {
        return () -> accessTokenEnabled;
    }

    private static class RecordingCredentialsFactory extends GcpCredentialsFactory {

        private Optional<String> serviceAccountLocation;
        private Optional<String> serviceAccountEncodedKey;
        private boolean accessTokenEnabled;

        @Override
        public Credentials create(Optional<String> serviceAccountLocation, Optional<String> serviceAccountEncodedKey,
                boolean accessTokenEnabled) {
            this.serviceAccountLocation = serviceAccountLocation;
            this.serviceAccountEncodedKey = serviceAccountEncodedKey;
            this.accessTokenEnabled = accessTokenEnabled;
            return NoCredentials.getInstance();
        }
    }
}
