package io.quarkiverse.googlecloudservices.it;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;

@Path("/secretmanager")
public class SecretManagerResource {

    @ConfigProperty(name = "quarkus.google.cloud.project-id")
    String projectId;

    @Inject
    SecretManagerServiceClient client;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String secretManager() {
        SecretVersionName secretVersionName = SecretVersionName.of(projectId, "integration-test", "latest");
        AccessSecretVersionResponse response = client.accessSecretVersion(secretVersionName);
        return response.getPayload().getData().toStringUtf8();
    }

}
