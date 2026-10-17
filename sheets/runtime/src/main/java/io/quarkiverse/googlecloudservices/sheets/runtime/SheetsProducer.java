package io.quarkiverse.googlecloudservices.sheets.runtime;

import java.io.IOException;
import java.security.GeneralSecurityException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.auth.Credentials;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import io.quarkus.arc.Unremovable;

@ApplicationScoped
public class SheetsProducer {
    @Inject
    Instance<Credentials> googleCredentials;

    @Inject
    SheetsConfiguration configuration;

    @Produces
    @Singleton
    @Default
    @Unremovable
    public Sheets sheets() throws GeneralSecurityException, IOException {
        Credentials credentials = googleCredentials.get();
        if (credentials instanceof GoogleCredentials googleCreds) {
            credentials = googleCreds.createScoped(configuration.scopes());
        }

        var builder = new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials));

        configuration.applicationName().ifPresent(builder::setApplicationName);
        configuration.rootUrl().ifPresent(builder::setRootUrl);
        return builder.build();
    }
}
