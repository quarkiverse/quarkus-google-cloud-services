package io.quarkiverse.googlecloudservices.bigquery.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

import java.util.Optional;

@ConfigMapping(prefix = "quarkus.google.cloud.bigquery")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface BigQueryConfiguration {
    /**
     * Overrides the default service host.
     * This is most commonly used for development or testing activities with a local Google Cloud BigQuery emulator instance.
     */
    Optional<String> hostOverride();
}
