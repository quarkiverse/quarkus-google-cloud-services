package io.quarkiverse.googlecloudservices.secretmanager.runtime.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.auth.Credentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

import io.quarkiverse.googlecloudservices.common.GcpBootstrapConfiguration;
import io.quarkiverse.googlecloudservices.secretmanager.runtime.SecretManagerConfiguration;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

class SecretManagerConfigSourceTest {

    private static final String SERVICE_ACCOUNT_LOCATION = "quarkus.google.cloud.secret-manager.service-account-location";
    private static final String SERVICE_ACCOUNT_ENCODED_KEY = "quarkus.google.cloud.secret-manager.service-account-encoded-key";

    @Test
    void usesServiceAccountLocationConfiguredForSecretManager(@TempDir Path tempDir) throws Exception {
        Path globalServiceAccount = writeServiceAccount(tempDir, "global-service-account.json", "global-project");
        Path secretManagerServiceAccount = writeServiceAccount(tempDir, "secret-manager-service-account.json",
                "secret-manager-project");

        Credentials credentials = SecretManagerConfigSource.credentials(
                globalConfiguration(Optional.of(globalServiceAccount)),
                configuration(SERVICE_ACCOUNT_LOCATION, secretManagerServiceAccount.toString()));

        assertThat(projectIdOf(credentials)).isEqualTo("secret-manager-project");
    }

    @Test
    void usesServiceAccountEncodedKeyConfiguredForSecretManager(@TempDir Path tempDir) throws Exception {
        Path globalServiceAccount = writeServiceAccount(tempDir, "global-service-account.json", "global-project");
        Path secretManagerServiceAccount = writeServiceAccount(tempDir, "secret-manager-service-account.json",
                "secret-manager-project");
        String encodedKey = Base64.getEncoder().encodeToString(Files.readAllBytes(secretManagerServiceAccount));

        Credentials credentials = SecretManagerConfigSource.credentials(
                globalConfiguration(Optional.of(globalServiceAccount)),
                configuration(SERVICE_ACCOUNT_ENCODED_KEY, encodedKey));

        assertThat(projectIdOf(credentials)).isEqualTo("secret-manager-project");
    }

    @Test
    void fallsBackToTheGlobalServiceAccount(@TempDir Path tempDir) throws Exception {
        Path globalServiceAccount = writeServiceAccount(tempDir, "global-service-account.json", "global-project");

        Credentials credentials = SecretManagerConfigSource.credentials(
                globalConfiguration(Optional.of(globalServiceAccount)),
                configuration());

        assertThat(projectIdOf(credentials)).isEqualTo("global-project");
    }

    private static String projectIdOf(Credentials credentials) {
        assertThat(credentials).isInstanceOf(ServiceAccountCredentials.class);
        return ((ServiceAccountCredentials) credentials).getProjectId();
    }

    private static SecretManagerConfiguration configuration(String... properties) {
        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder().withMapping(SecretManagerConfiguration.class);
        for (int i = 0; i < properties.length; i += 2) {
            builder.withDefaultValue(properties[i], properties[i + 1]);
        }
        SmallRyeConfig config = builder.build();
        return config.getConfigMapping(SecretManagerConfiguration.class);
    }

    private static GcpBootstrapConfiguration globalConfiguration(Optional<Path> serviceAccountLocation) {
        GcpBootstrapConfiguration configuration = mock(GcpBootstrapConfiguration.class);
        when(configuration.serviceAccountLocation())
                .thenReturn(serviceAccountLocation.map(Path::toString));
        when(configuration.serviceAccountEncodedKey()).thenReturn(Optional.empty());
        return configuration;
    }

    private static Path writeServiceAccount(Path directory, String fileName, String projectId) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        String privateKey = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, new byte[] { '\n' }).encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";

        String json = """
                {
                  "type": "service_account",
                  "project_id": "__PROJECT_ID__",
                  "private_key_id": "test-key-id",
                  "private_key": "__PRIVATE_KEY__",
                  "client_email": "test@__PROJECT_ID__.iam.gserviceaccount.com",
                  "client_id": "123456789",
                  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
                  "token_uri": "https://oauth2.googleapis.com/token",
                  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
                  "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/test@__PROJECT_ID__.iam.gserviceaccount.com"
                }
                """;

        Path file = directory.resolve(fileName);
        Files.writeString(file, json
                .replace("__PROJECT_ID__", projectId)
                .replace("__PRIVATE_KEY__", privateKey.replace("\n", "\\n")));
        return file;
    }
}
