package io.quarkiverse.googlecloudservices.secretmanager.runtime;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;

import io.quarkiverse.googlecloudservices.common.GcpConfiguration;
import io.quarkus.credentials.CredentialsProvider;

public class SecretManagerCredentialsProvider implements CredentialsProvider {
    @Inject
    GcpConfiguration gcpConfiguration;

    @Inject
    SecretManagerCredentialsProviderConfig providerConfig;

    @Override
    public Map<String, String> getCredentials(String credentialsProviderName) {
        String projectId = gcpConfiguration.projectId
                .orElseThrow(() -> new SecretManagerCredentialProviderException("Google Cloud project ID must be set."));

        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            String secretName = providerConfig.secretName;
            String secretVersion = providerConfig.secretVersion;
            SecretVersionName secretVersionName = SecretVersionName.of(projectId, secretName, secretVersion);
            AccessSecretVersionResponse response = client.accessSecretVersion(secretVersionName);
            String password = response.getPayload().getData().toStringUtf8();
            return Collections.singletonMap(CredentialsProvider.PASSWORD_PROPERTY_NAME, password);
        } catch (IOException e) {
            throw new SecretManagerCredentialProviderException("Unable to retrieve credentials using secret manager", e);
        }
    }
}
