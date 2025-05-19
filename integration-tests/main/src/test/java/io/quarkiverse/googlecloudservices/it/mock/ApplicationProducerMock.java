package io.quarkiverse.googlecloudservices.it.mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.mockito.Mockito;

import com.google.auth.Credentials;
import com.google.cloud.NoCredentials;
import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretPayload;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import com.google.protobuf.ByteString;

import io.quarkus.test.Mock;

@Mock
@ApplicationScoped
public class ApplicationProducerMock {

    @Produces
    @Singleton
    @Default
    public Credentials googleCredential() {
        return NoCredentials.getInstance();
    }

    @Produces
    @Singleton
    @Default
    public SecretManagerServiceClient secretManagerServiceClient() {
        SecretManagerServiceClient client = Mockito.mock(SecretManagerServiceClient.class);
        when(client.accessSecretVersion(any(SecretVersionName.class)))
                .thenReturn(
                        AccessSecretVersionResponse.newBuilder()
                                .setName("test-secret")
                                .setPayload(SecretPayload.newBuilder().setData(ByteString.copyFromUtf8("hello")))
                                .build());
        return client;
    }
}
