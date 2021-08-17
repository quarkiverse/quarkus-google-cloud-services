package io.quarkiverse.googlecloudservices.common;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;

@Singleton
public class GcpCredentialProviderProducer {

    @Inject
    GoogleCredentials googleCredentials;

    @Produces
    @Singleton
    @Default
    public CredentialsProvider credentialsProvider() {
        return FixedCredentialsProvider.create(googleCredentials);
    }
}
