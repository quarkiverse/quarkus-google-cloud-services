package io.quarkiverse.googlecloudservices.storage.runtime;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

import com.google.api.Logging;
import com.google.auth.oauth2.GoogleCredentials;

import io.quarkiverse.googlecloudservices.common.GcpConfigHolder;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;

public class LoggingHandler extends Handler {

    private LoggingConfiguration config;

    public LoggingHandler(LoggingConfiguration config) {
        this.config = config;
    }

    @Override
    public void close() throws SecurityException {

    }

    @Override
    public void publish(LogRecord record) {
        System.out.println("Log Record published: " + record);
    }

    @Override
    public void flush() {

    }

    private Logging checkCreateLogging() {
        InstanceHandle<GcpConfigHolder> config = Arc.container().instance(GcpConfigHolder.class);
        InstanceHandle<GoogleCredentials> creds = Arc.container().instance(GoogleCredentials.class);

        /*
         * GcpBootstrapConfiguration gcpConfiguration = gcpConfigHolder.getBootstrapConfig();
         * LoggingOptions options = LoggingOptions.getDefaultInstance().toBuilder()
         * .setCredentials(googleCredentials)
         * .setProjectId(gcpConfiguration.projectId.orElse(null))
         * .build();
         */

        return null;
    }
}
