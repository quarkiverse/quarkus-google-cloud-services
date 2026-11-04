package io.quarkiverse.googlecloudservices.secretmanager.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import io.quarkiverse.googlecloudservices.common.GcpBootstrapConfiguration;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

class SecretManagerConfigurationTest {

    private static final String ACCESS_TOKEN_ENABLED = "quarkus.google.cloud.secret-manager.access-token-enabled";

    @Test
    void accessTokenEnabledIsEmptyWhenNotConfigured() {
        assertThat(configuration().accessTokenEnabled()).isEmpty();
    }

    @Test
    void accessTokenEnabledIsReadFromConfiguration() {
        assertThat(configuration(ACCESS_TOKEN_ENABLED, "false").accessTokenEnabled()).contains(false);
        assertThat(configuration(ACCESS_TOKEN_ENABLED, "true").accessTokenEnabled()).contains(true);
    }

    @Test
    void inheritsGlobalAccessTokenConfigurationWhenNotConfigured() {
        assertThat(configuration().effectiveAccessTokenEnabled(globalConfiguration(true))).isTrue();
        assertThat(configuration().effectiveAccessTokenEnabled(globalConfiguration(false))).isFalse();
    }

    @Test
    void overridesGlobalAccessTokenConfiguration() {
        assertThat(configuration(ACCESS_TOKEN_ENABLED, "false").effectiveAccessTokenEnabled(globalConfiguration(true)))
                .isFalse();
        assertThat(configuration(ACCESS_TOKEN_ENABLED, "true").effectiveAccessTokenEnabled(globalConfiguration(false)))
                .isTrue();
    }

    private static SecretManagerConfiguration configuration(String... properties) {
        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder().withMapping(SecretManagerConfiguration.class);
        for (int i = 0; i < properties.length; i += 2) {
            builder.withDefaultValue(properties[i], properties[i + 1]);
        }
        SmallRyeConfig config = builder.build();
        return config.getConfigMapping(SecretManagerConfiguration.class);
    }

    private static GcpBootstrapConfiguration globalConfiguration(boolean accessTokenEnabled) {
        GcpBootstrapConfiguration configuration = mock(GcpBootstrapConfiguration.class);
        when(configuration.accessTokenEnabled()).thenReturn(accessTokenEnabled);
        return configuration;
    }
}
