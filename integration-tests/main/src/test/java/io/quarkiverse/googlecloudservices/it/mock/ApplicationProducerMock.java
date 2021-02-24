package io.quarkiverse.googlecloudservices.it.mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.mockito.Mockito;

import com.google.auth.oauth2.GoogleCredentials;
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
    public GoogleCredentials googleCredential() throws IOException {
        return Mockito.mock(GoogleCredentials.class);
    }

    @Produces
    @Singleton
    @Default
    public SecretManagerServiceClient secretManagerServiceClient() throws IOException {
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
