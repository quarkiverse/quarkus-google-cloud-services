package io.quarkiverse.googlecloudservices.firebase.deployment.testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@Disabled("Temporarily for the release, see https://github.com/quarkiverse/quarkus-google-cloud-services/issues/754")
public class FirebaseEmulatorContainerCustomConfigTest {

    private static final File tempEmulatorDataDir;

    static {
        try {
            // Create a temporary directory for emulator data
            tempEmulatorDataDir = new File("target/firebase-emulator-container-data");
            tempEmulatorDataDir.mkdirs();
            var testContainer = new TestableFirebaseEmulatorContainer("FirebaseEmulatorContainerCustomConfigTest");
            firebaseContainer = testContainer.testBuilder()
                    .withCliArguments()
                    .withEmulatorData(tempEmulatorDataDir.toPath())
                    .withExperiments(Set.of("webframeworks"))
                    .done()
                    .readFromFirebaseJson(new File("src/test/firebase.json").toPath())
                    .build();

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Container
    private static final FirebaseEmulatorContainer firebaseContainer;

    @Test
    public void testFirestoreRulesAndIndexes() throws InterruptedException, IOException {
        // Verify the firebase.json file exists in the container
        String firebaseJsonCheck = firebaseContainer.execInContainer("cat", "/srv/firebase/firebase.json").getStdout();
        assertTrue(firebaseJsonCheck.contains("\"emulators\""), "Expected firebase.json to be present in the container");

        // Verify the firestore.rules file exists in the container
        String firestoreRulesCheck = firebaseContainer.execInContainer("cat", "/srv/firebase/firestore.rules").getStdout();
        assertTrue(firestoreRulesCheck.contains("service cloud.firestore"),
                "Expected firestore.rules to be present in the container");
    }

    @Test
    public void testStorageRules() throws IOException, InterruptedException {
        // Verify the storage.rules file exists in the container
        String storageRulesCheck = firebaseContainer.execInContainer("cat", "/srv/firebase/storage.rules").getStdout();
        assertTrue(storageRulesCheck.contains("service firebase.storage"),
                "Expected storage.rules to be present in the container");
    }

    @Test
    public void testHosting() throws IOException, InterruptedException, URISyntaxException {
        HttpClient httpClient = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
                .GET()
                .uri(new URI("http://localhost:7006/test.me"))
                .build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        var body = response.body();
        assertEquals("This is a test file for hosting", body);
    }

    @Test
    public void testFunctions() throws IOException, InterruptedException, URISyntaxException {
        HttpClient httpClient = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
                .GET()
                .uri(new URI("http://localhost:7007/demo-test-project/us-central1/helloworld"))
                .build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        var body = response.body();
        assertEquals("Hello world", body);
    }

}
