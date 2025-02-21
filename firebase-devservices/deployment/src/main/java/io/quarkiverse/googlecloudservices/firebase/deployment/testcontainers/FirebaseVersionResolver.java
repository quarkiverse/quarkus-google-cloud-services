package io.quarkiverse.googlecloudservices.firebase.deployment.testcontainers;

import com.google.api.client.json.gson.GsonFactory;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves the version of the firebase tools based on the NPM registry in case version
 * "latest" is used.
 */
public class FirebaseVersionResolver {

    public String resolveVersion(String version) throws IOException {
        if (!Objects.equals(version, "latest")) {
            return version;
        }

        try (var httpClient = HttpClientBuilder.create().build()) {
            var request = new HttpGet("https://registry.npmjs.org/firebase-tools/latest");
            var response = httpClient.execute(request);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IOException("Unexpected response code " + response.getStatusLine().getStatusCode());
            }
            GsonFactory gsonFactory = GsonFactory.getDefaultInstance();

            try (var json = gsonFactory.createJsonParser(response.getEntity().getContent())) {
                var npmResponse = json.parse(Map.class);
                if (!npmResponse.containsKey("version") || npmResponse.get("version") == null) {
                    throw new IOException("Unexpected response from NPM API");
                }

                return npmResponse.get("version").toString();
            }
        }
    }
}
