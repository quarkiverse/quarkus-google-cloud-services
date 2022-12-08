package io.quarkiverse.googlecloudservices.it;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;

@Path("/secretmanager")
public class SecretManagerResource {

    @Inject
    SecretManagerServiceClient client;

    @ConfigProperty(name = "quarkus.google.cloud.project-id")
    String projectId;

    @ConfigProperty(name = "my.database.password")
    String secret;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String secretManager() {
        SecretVersionName secretVersionName = SecretVersionName.of(projectId, "integration-test", "latest");
        AccessSecretVersionResponse response = client.accessSecretVersion(secretVersionName);

        return String.format(
                "Secret accessed via client: %s || Secret accessed via property: %s",
                response.getPayload().getData().toStringUtf8(), secret);
    }

}
