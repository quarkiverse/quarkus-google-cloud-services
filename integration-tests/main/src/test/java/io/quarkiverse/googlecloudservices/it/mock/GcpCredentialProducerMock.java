package io.quarkiverse.googlecloudservices.it.mock;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.mockito.Mockito;

import com.google.auth.oauth2.GoogleCredentials;

import io.quarkus.test.Mock;

@Mock
@ApplicationScoped
public class GcpCredentialProducerMock {
    @Produces
    @Singleton
    @Default
    public GoogleCredentials googleCredential() throws IOException {
        return Mockito.mock(GoogleCredentials.class);
    }
}
