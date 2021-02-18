package io.quarkiverse.googlecloudservices.secretmanager.runtime.config;

import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import io.quarkus.arc.Arc;

/**
 * Provides an instance of {@link SecretManagerServiceClient} from the Quarkus application context
 * with concurrency protection.
 */
class SecretManagerClientProvider {

    private volatile SecretManagerServiceClient client = null;

    synchronized SecretManagerServiceClient get() {
        if (client == null) {
            // Retrieve the Secret Manager client in the context.
            client = Arc.container().instance(SecretManagerServiceClient.class).get();
        }
        return client;
    }
}
