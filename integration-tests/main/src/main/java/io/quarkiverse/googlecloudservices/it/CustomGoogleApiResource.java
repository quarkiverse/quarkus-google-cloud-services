package io.quarkiverse.googlecloudservices.it;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.auth.Credentials;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

/**
 * Example of calling a Google API that does not have a dedicated extension, by reusing the
 * {@link Credentials} bean produced by the extension. The request is sent to a local stub so
 * the example can run without live Google services.
 */
@Path("/custom-api")
public class CustomGoogleApiResource {

    @Inject
    Credentials credentials;

    @ConfigProperty(name = "quarkus.google.cloud.project-id")
    String projectId;

    @ConfigProperty(name = "custom-api.root-url")
    String rootUrl;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String call() throws IOException {
        NetHttpTransport transport = new NetHttpTransport();

        HttpRequestFactory requestFactory;
        if (credentials instanceof GoogleCredentials googleCredentials) {
            requestFactory = transport.createRequestFactory(new HttpCredentialsAdapter(googleCredentials));
        } else {
            // NoCredentials, used with emulators and tests, is not compatible with HttpCredentialsAdapter
            requestFactory = transport.createRequestFactory();
        }

        HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(rootUrl + "v1/custom"));
        request.getHeaders().set("X-Project-Id", projectId);
        HttpResponse response = request.execute();
        return response.parseAsString();
    }
}
