package io.quarkiverse.googlecloudservices.secretmanager.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkiverse.googlecloudservices.common.GcpBootstrapConfiguration;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

class SecretManagerConfigurationTest {

    private static final String PREFIX = "quarkus.google.cloud.secret-manager.";
    private static final String SERVICE_ACCOUNT_LOCATION = PREFIX + "service-account-location";
    private static final String SERVICE_ACCOUNT_ENCODED_KEY = PREFIX + "service-account-encoded-key";
    private static final String ACCESS_TOKEN_ENABLED = PREFIX + "access-token-enabled";

    @Test
    void propertiesAreEmptyWhenNotConfigured() {
        SecretManagerConfiguration configuration = configuration();

        assertThat(configuration.serviceAccountLocation()).isEmpty();
        assertThat(configuration.serviceAccountEncodedKey()).isEmpty();
        assertThat(configuration.accessTokenEnabled()).isEmpty();
    }

    @Test
    void propertiesAreReadFromConfiguration() {
        SecretManagerConfiguration configuration = configuration(
                SERVICE_ACCOUNT_LOCATION, "/path/service-account.json",
                SERVICE_ACCOUNT_ENCODED_KEY, "encoded-key",
                ACCESS_TOKEN_ENABLED, "false");

        assertThat(configuration.serviceAccountLocation()).contains("/path/service-account.json");
        assertThat(configuration.serviceAccountEncodedKey()).contains("encoded-key");
        assertThat(configuration.accessTokenEnabled()).contains(false);
    }

    @Test
    void inheritsGlobalAccessTokenConfigurationWhenNotConfigured() {
        assertThat(configuration().effectiveAccessTokenEnabled(globalConfiguration(null, null, true))).isTrue();
        assertThat(configuration().effectiveAccessTokenEnabled(globalConfiguration(null, null, false))).isFalse();
    }

    @Test
    void overridesGlobalAccessTokenConfiguration() {
        assertThat(configuration(ACCESS_TOKEN_ENABLED, "false")
                .effectiveAccessTokenEnabled(globalConfiguration(null, null, true))).isFalse();
        assertThat(configuration(ACCESS_TOKEN_ENABLED, "true")
                .effectiveAccessTokenEnabled(globalConfiguration(null, null, false))).isTrue();
    }

    @Test
    void inheritsGlobalServiceAccountConfigurationWhenNotConfigured() {
        SecretManagerConfiguration configuration = configuration();

        assertThat(configuration.hasServiceAccountConfiguration()).isFalse();
        assertThat(configuration.effectiveServiceAccountLocation(globalConfiguration("/global.json", "global-key", true)))
                .contains("/global.json");
        assertThat(configuration.effectiveServiceAccountEncodedKey(globalConfiguration("/global.json", "global-key", true)))
                .contains("global-key");
    }

    @Test
    void serviceAccountLocationOverridesGlobalConfiguration() {
        SecretManagerConfiguration configuration = configuration(SERVICE_ACCOUNT_LOCATION, "/service.json");

        assertThat(configuration.hasServiceAccountConfiguration()).isTrue();
        assertThat(configuration.effectiveServiceAccountLocation(globalConfiguration("/global.json", "global-key", true)))
                .contains("/service.json");
        assertThat(configuration.effectiveServiceAccountEncodedKey(globalConfiguration("/global.json", "global-key", true)))
                .isEmpty();
    }

    @Test
    void serviceAccountEncodedKeyOverridesGlobalConfigurationAsAWhole() {
        SecretManagerConfiguration configuration = configuration(SERVICE_ACCOUNT_ENCODED_KEY, "service-key");

        assertThat(configuration.effectiveServiceAccountLocation(globalConfiguration("/global.json", "global-key", true)))
                .isEmpty();
        assertThat(configuration.effectiveServiceAccountEncodedKey(globalConfiguration("/global.json", "global-key", true)))
                .contains("service-key");
    }

    private static SecretManagerConfiguration configuration(String... properties) {
        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder().withMapping(SecretManagerConfiguration.class);
        for (int i = 0; i < properties.length; i += 2) {
            builder.withDefaultValue(properties[i], properties[i + 1]);
        }
        SmallRyeConfig config = builder.build();
        return config.getConfigMapping(SecretManagerConfiguration.class);
    }

    private static GcpBootstrapConfiguration globalConfiguration(String serviceAccountLocation,
            String serviceAccountEncodedKey, boolean accessTokenEnabled) {
        GcpBootstrapConfiguration configuration = mock(GcpBootstrapConfiguration.class);
        when(configuration.serviceAccountLocation()).thenReturn(Optional.ofNullable(serviceAccountLocation));
        when(configuration.serviceAccountEncodedKey()).thenReturn(Optional.ofNullable(serviceAccountEncodedKey));
        when(configuration.accessTokenEnabled()).thenReturn(accessTokenEnabled);
        return configuration;
    }
}
