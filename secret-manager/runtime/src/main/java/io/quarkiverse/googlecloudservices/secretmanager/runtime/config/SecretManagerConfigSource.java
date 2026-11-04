package io.quarkiverse.googlecloudservices.secretmanager.runtime.config;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretManagerServiceSettings;
import com.google.cloud.secretmanager.v1.SecretVersionName;

import io.quarkiverse.googlecloudservices.common.GcpBootstrapConfiguration;
import io.quarkiverse.googlecloudservices.common.GcpCredentials;
import io.smallrye.config.common.AbstractConfigSource;

public class SecretManagerConfigSource extends AbstractConfigSource {

    /** The ordinal is set to < 100 (which is the default) so that this config source is retrieved from last. */
    private static final int SECRET_MANAGER_ORDINAL = 50;

    private static final String CONFIG_SOURCE_NAME = "io.quarkiverse.googlecloudservices.secretmanager.runtime.config";

    private final String projectId;
    private final SecretManagerServiceClient client;
    private final AtomicBoolean closed;

    public SecretManagerConfigSource(final GcpBootstrapConfiguration gcpConfig, final String projectId) {
        super(CONFIG_SOURCE_NAME, SECRET_MANAGER_ORDINAL);
        this.projectId = projectId;
        if (gcpConfig.secretManagerEnabled()) {
            this.client = createClient(gcpConfig, projectId);
            this.closed = new AtomicBoolean(false);
        } else {
            this.client = null;
            this.closed = new AtomicBoolean(true);
        }
    }

    @Override
    public String getValue(String propertyName) {
        SecretVersionName secretVersionName = SecretManagerConfigUtils.getSecretVersionName(propertyName, projectId);
        if (secretVersionName == null) {
            // The propertyName is not in the form "${sm//...}" so return null.
            return null;
        }

        if (!closed.get()) {
            AccessSecretVersionResponse response = client.accessSecretVersion(secretVersionName);
            return response.getPayload().getData().toStringUtf8();
        }

        return null;
    }

    @Override
    public Set<String> getPropertyNames() {
        return Collections.emptySet();
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.emptyMap();
    }

    void closeClient() {
        closed.compareAndSet(false, true);
        if (client != null) {
            client.close();
        }
    }

    private static SecretManagerServiceClient createClient(
            final GcpBootstrapConfiguration gcpConfig,
            final String projectId) {

        try {
            return SecretManagerServiceClient.create(
                    SecretManagerServiceSettings.newBuilder()
                            .setQuotaProjectId(projectId)
                            .setCredentialsProvider(FixedCredentialsProvider.create(credentials(gcpConfig)))
                            .build());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Credentials credentials(final GcpBootstrapConfiguration gcpConfig) throws IOException {
        Credentials serviceAccountCredentials = GcpCredentials.fromServiceAccount(gcpConfig.serviceAccountLocation(),
                gcpConfig.serviceAccountEncodedKey());
        if (serviceAccountCredentials != null) {
            return serviceAccountCredentials;
        }
        return GcpCredentials.applicationDefault();
    }
}
