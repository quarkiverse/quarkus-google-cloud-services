package io.quarkiverse.googlecloudservices.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigValue;

class GcpDefaultsConfigSourceFactoryTest {
    @Test
    void configSourceWorks() {
        ConfigSourceContext context = Mockito.mock(ConfigSourceContext.class);
        Mockito.when(context.getValue("quarkus.google.cloud.enable-metadata-server"))
                .thenReturn(ConfigValue.builder().withValue("true").build());

        Iterable<ConfigSource> configSources = new GcpDefaultsConfigSourceFactory(() -> "test-project-id")
                .getConfigSources(context);
        assertThat(configSources).asList().hasSize(1);

        ConfigSource configSource = configSources.iterator().next();
        assertThat(configSource.getProperties()).containsEntry("quarkus.google.cloud.project-id", "test-project-id");
    }

    @Test
    void metadataServerDisabled() {
        ConfigSourceContext context = Mockito.mock(ConfigSourceContext.class);
        Mockito.when(context.getValue("quarkus.google.cloud.enable-metadata-server"))
                .thenReturn(ConfigValue.builder().withValue("false").build());

        Iterable<ConfigSource> configSources = new GcpDefaultsConfigSourceFactory(() -> "test-project-id")
                .getConfigSources(context);
        assertThat(configSources).isEmpty();
    }

    /**
     * Tests that OpenCensus does not get implicitly initialized and in turn does not "collide" with
     * OpenTelemetry, as used in Quarkus.
     */
    @Test
    void staticOpenCensusOpenTelemetryInit() {
        try {
            GlobalOpenTelemetry.resetForTest();

            ConfigSourceContext context = Mockito.mock(ConfigSourceContext.class);
            Mockito.when(context.getValue("quarkus.google.cloud.enable-metadata-server"))
                    .thenReturn(ConfigValue.builder().withValue("true").build());

            // Uses the "real" implementation that tries to fetch the default-project-id
            Iterable<ConfigSource> configSources = new GcpDefaultsConfigSourceFactory().getConfigSources(context);
            assertThat(configSources).asList().hasSizeLessThanOrEqualTo(1);

            // This is a pretty ugly way, because it changes static state
            GlobalOpenTelemetry.set(OpenTelemetry.noop());
        } finally {
            GlobalOpenTelemetry.resetForTest();
        }
    }
}
