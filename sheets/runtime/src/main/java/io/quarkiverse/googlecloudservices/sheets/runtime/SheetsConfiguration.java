package io.quarkiverse.googlecloudservices.sheets.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.google.cloud.sheets")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface SheetsConfiguration {
    /**
     * OAuth scopes requested for the Google credentials.
     * Use `https://www.googleapis.com/auth/spreadsheets.readonly` for read-only access.
     */
    @WithDefault("https://www.googleapis.com/auth/spreadsheets")
    List<String> scopes();

    /**
     * Application name sent to Google in the User-Agent header of each request.
     */
    Optional<String> applicationName();

    /**
     * Override the root URL of the API, for example to point the client to a stub in tests.
     */
    Optional<String> rootUrl();
}
