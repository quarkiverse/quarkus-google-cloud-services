package io.quarkiverse.googlecloudservices.firebase.deployment.testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.google.firebase.FirebaseOptions;

@Testcontainers
public class FirebaseEmulatorContainerCustomConfigTest {

    private static final File tempEmulatorDataDir;

    static {
        try {
            // Create a temporary directory for emulator data
            tempEmulatorDataDir = Files.createTempDirectory("firebase-emulator-data").toFile();
            firebaseContainer = new TestableFirebaseEmulatorContainer(
                    new FirebaseEmulatorContainer.EmulatorConfig(
                            new FirebaseEmulatorContainer.DockerConfig(
                                    "node:23-alpine",
                                    TestableFirebaseEmulatorContainer.user,
                                    TestableFirebaseEmulatorContainer.group),
                            "latest", // Firebase version
                            Optional.of("demo-test-project"),
                            Optional.empty(),
                            Optional.of(new File("firebase.json").toPath()),
                            Optional.empty(),
                            Optional.of(tempEmulatorDataDir.toPath()),
                            new CustomFirebaseConfigReader().readFromFirebase(new File("firebase.json").toPath())),
                    "FirebaseEmulatorContainerCustomConfigTest") {

                @Override
                protected void createFirebaseOptions(FirebaseOptions.Builder builder) {
                }
            };

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

}
