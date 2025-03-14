package io.quarkiverse.googlecloudservices.pubsub.push;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

/**
 * Default implementation of {@link TokenVerifier} using the {@link GoogleIdTokenVerifier}.
 */
@ApplicationScoped
public class GoogleTokenVerifier implements TokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    @Inject
    public GoogleTokenVerifier(
            @ConfigProperty(name = "quarkus.google.cloud.pubsub.push.audience") Optional<String> pubsubAudience) {
        var builder = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                new GsonFactory());

        pubsubAudience.ifPresent(audience -> {
            var audiences = Arrays.stream(audience.split(",")).toList();
            builder.setAudience(audiences);
        });

        this.verifier = builder.build();
    }

    @Override
    public GoogleIdToken verify(String token) throws GeneralSecurityException, IOException {
        return verifier.verify(token);
    }
}
