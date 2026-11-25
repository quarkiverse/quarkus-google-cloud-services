package io.quarkiverse.googlecloudservices.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.inject.Instance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.NoCredentials;

import io.quarkus.security.credential.Credential;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.SecurityIdentity;

@SuppressWarnings("unchecked")
class GcpCredentialsFactoryTest {

    @Test
    void forwardsAccessTokenWhenEnabled() throws Exception {
        SecurityIdentity identity = identityWithBearerToken("first-user-token");

        GcpCredentialsFactory factory = factoryWithApplicationDefault(NoCredentials.getInstance());
        factory.securityIdentity = resolvable(identity);

        Credentials credentials = factory.create(Optional.empty(), Optional.empty(), true);

        assertThat(credentials).isInstanceOf(GoogleCredentials.class);
        assertThat(((GoogleCredentials) credentials).getAccessToken().getTokenValue()).isEqualTo("first-user-token");
    }

    @Test
    void doesNotUseAccessTokenWhenDisabled() throws Exception {
        Instance<SecurityIdentity> identities = mock(Instance.class);

        GcpCredentialsFactory factory = factoryWithApplicationDefault(NoCredentials.getInstance());
        factory.securityIdentity = identities;

        Credentials credentials = factory.create(Optional.empty(), Optional.empty(), false);

        assertThat(credentials).isSameAs(NoCredentials.getInstance());
        verifyNoInteractions(identities);
    }

    @Test
    void doesNotUseAccessTokenWhenIdentityIsAnonymous() throws Exception {
        SecurityIdentity identity = identityWithBearerToken("first-user-token");
        when(identity.isAnonymous()).thenReturn(true);

        GcpCredentialsFactory factory = factoryWithApplicationDefault(NoCredentials.getInstance());
        factory.securityIdentity = resolvable(identity);

        Credentials credentials = factory.create(Optional.empty(), Optional.empty(), true);

        assertThat(credentials).isSameAs(NoCredentials.getInstance());
    }

    @Test
    void doesNotUseAccessTokenWhenIdentityIsNotResolvable() throws Exception {
        Instance<SecurityIdentity> identities = mock(Instance.class);
        when(identities.isResolvable()).thenReturn(false);

        GcpCredentialsFactory factory = factoryWithApplicationDefault(NoCredentials.getInstance());
        factory.securityIdentity = identities;

        Credentials credentials = factory.create(Optional.empty(), Optional.empty(), true);

        assertThat(credentials).isSameAs(NoCredentials.getInstance());
    }

    @Test
    void serviceAccountTakesPrecedenceOverAccessToken(@TempDir Path tempDir) throws Exception {
        Path serviceAccount = writeServiceAccount(tempDir);
        Instance<SecurityIdentity> identities = mock(Instance.class);

        GcpCredentialsFactory factory = factoryWithApplicationDefault(NoCredentials.getInstance());
        factory.securityIdentity = identities;

        Credentials credentials = factory.create(Optional.of(serviceAccount.toString()), Optional.empty(), true);

        assertThat(credentials).isInstanceOf(ServiceAccountCredentials.class);
        verifyNoInteractions(identities);
    }

    @Test
    void returnsNullServiceAccountWhenNothingIsConfigured() throws Exception {
        assertThat(GcpCredentials.fromServiceAccount(Optional.empty(), Optional.empty())).isNull();
    }

    private static GcpCredentialsFactory factoryWithApplicationDefault(Credentials applicationDefault) {
        return new GcpCredentialsFactory() {
            @Override
            Credentials applicationDefault() {
                return applicationDefault;
            }
        };
    }

    private static Instance<SecurityIdentity> resolvable(SecurityIdentity identity) {
        Instance<SecurityIdentity> identities = mock(Instance.class);
        when(identities.isResolvable()).thenReturn(true);
        when(identities.get()).thenReturn(identity);
        return identities;
    }

    private static SecurityIdentity identityWithBearerToken(String token) {
        SecurityIdentity identity = mock(SecurityIdentity.class);
        when(identity.isAnonymous()).thenReturn(false);
        when(identity.getCredentials()).thenReturn(Set.<Credential> of(new TokenCredential(token, "bearer")));
        return identity;
    }

    private static Path writeServiceAccount(Path directory) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        String privateKey = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, new byte[] { '\n' }).encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";

        String json = """
                {
                  "type": "service_account",
                  "project_id": "test-project",
                  "private_key_id": "test-key-id",
                  "private_key": "__PRIVATE_KEY__",
                  "client_email": "test@test-project.iam.gserviceaccount.com",
                  "client_id": "123456789",
                  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
                  "token_uri": "https://oauth2.googleapis.com/token",
                  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
                  "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/test@test-project.iam.gserviceaccount.com"
                }
                """;

        Path file = directory.resolve("service-account.json");
        Files.writeString(file, json.replace("__PRIVATE_KEY__", privateKey.replace("\n", "\\n")));
        return file;
    }
}
