package io.quarkiverse.googlecloudservices.firebase.deployment;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkiverse.googlecloudservices.firebase.deployment.testcontainers.FirebaseEmulatorContainer;

class FirebaseEmulatorConfigBuilderTest {

    private FirebaseEmulatorConfigBuilder configBuilder;

    @BeforeEach
    void setUp() {
        // Storage
        FirebaseDevServiceConfig config = new TestFirebaseDevServiceConfig(
                Optional.of("my-project-id"),
                new TestFirebase(
                        new TestFirebaseDevService(
                                true,
                                "node:21-alpine",
                                Optional.empty(),
                                Optional.empty(),
                                "11.0.0",
                                Optional.of("MY_TOKEN"),
                                Optional.of("firebase.json"),
                                Optional.of("-Xmx"),
                                Optional.of("data"),
                                new TestUI(
                                        true,
                                        Optional.of(6000),
                                        Optional.of(6001),
                                        Optional.of(6002))),
                        new TestAuth(
                                new TestGenericDevService(true, Optional.of(6003))),
                        new TestHosting(
                                new TestGenericDevService(true, Optional.of(6004)),
                                Optional.of("public"))),
                new TestDatabase(
                        new TestGenericDevService(
                                false,
                                Optional.of(6005))),
                new TestFirestore(
                        new TestFirestoreDevService(
                                true,
                                Optional.of(6006),
                                Optional.of(6007),
                                Optional.of("firestore.rules"),
                                Optional.of("firestore.indexes.json"))),
                new TestFunctions(
                        new TestGenericDevService(
                                true,
                                Optional.of(6008))),
                new TestPubSub(
                        new TestGenericDevService(
                                true,
                                Optional.of(6009))),
                new TestStorage(
                        new TestStorageDevService(
                                true,
                                Optional.empty(),
                                Optional.of("storage.rules"))));
        configBuilder = new FirebaseEmulatorConfigBuilder(config);
    }

    @Test
    void testBuild() {
        FirebaseEmulatorContainer.EmulatorConfig emulatorConfig = configBuilder.build();

        assertNotNull(emulatorConfig);
        assertEquals("node:21-alpine", emulatorConfig.dockerConfig().imageName());
        assertEquals("11.0.0", emulatorConfig.firebaseVersion());
        assertEquals("my-project-id", emulatorConfig.projectId().orElse(null));
        assertEquals("MY_TOKEN", emulatorConfig.token().orElse(null));
        assertPathEndsWith("firebase.json", emulatorConfig.customFirebaseJson().orElse(null));
        assertEquals("-Xmx", emulatorConfig.javaToolOptions().orElse(null));
        assertPathEndsWith("data", emulatorConfig.emulatorData().orElse(null));
        assertPathEndsWith("public", emulatorConfig.firebaseConfig().hostingConfig().hostingContentDir().orElse(null));
        assertPathEndsWith("storage.rules", emulatorConfig.firebaseConfig().storageConfig().rulesFile().orElse(null));
        assertPathEndsWith("firestore.rules", emulatorConfig.firebaseConfig().firestoreConfig().rulesFile().orElse(null));
        assertPathEndsWith("firestore.indexes.json",
                emulatorConfig.firebaseConfig().firestoreConfig().indexesFile().orElse(null));

    }

    private void assertPathEndsWith(String expected, Path path) {
        assertNotNull(path);
        assertTrue(path.toString().endsWith(expected));
    }

    @Test
    void testExposedEmulators() {
        FirebaseEmulatorContainer.EmulatorConfig emulatorConfig = configBuilder.build();

        Map<FirebaseEmulatorContainer.Emulator, FirebaseEmulatorContainer.ExposedPort> exposedPorts = emulatorConfig
                .firebaseConfig().services();

        assertEquals(10, exposedPorts.size());
        assertEquals(6000, exposedPorts.get(FirebaseEmulatorContainer.Emulator.EMULATOR_SUITE_UI).fixedPort());
        assertEquals(6001, exposedPorts.get(FirebaseEmulatorContainer.Emulator.LOGGING).fixedPort());
        assertEquals(6002, exposedPorts.get(FirebaseEmulatorContainer.Emulator.EMULATOR_HUB).fixedPort());
        assertEquals(6003, exposedPorts.get(FirebaseEmulatorContainer.Emulator.AUTHENTICATION).fixedPort());
        assertEquals(6004, exposedPorts.get(FirebaseEmulatorContainer.Emulator.FIREBASE_HOSTING).fixedPort());
        assertEquals(6006, exposedPorts.get(FirebaseEmulatorContainer.Emulator.CLOUD_FIRESTORE).fixedPort());
        assertEquals(6007, exposedPorts.get(FirebaseEmulatorContainer.Emulator.CLOUD_FIRESTORE_WS).fixedPort());
        assertEquals(6008, exposedPorts.get(FirebaseEmulatorContainer.Emulator.CLOUD_FUNCTIONS).fixedPort());
        assertEquals(6009, exposedPorts.get(FirebaseEmulatorContainer.Emulator.PUB_SUB).fixedPort());
        assertNull(exposedPorts.get(FirebaseEmulatorContainer.Emulator.CLOUD_STORAGE).fixedPort());

        assertNull(exposedPorts.get(FirebaseEmulatorContainer.Emulator.REALTIME_DATABASE));
    }

    // Record implementations for interfaces
    record TestFirebaseDevServiceConfig(
            Optional<String> projectId,
            Firebase firebase,
            Database database,
            Firestore firestore,
            Functions functions,
            PubSub pubsub,
            Storage storage) implements FirebaseDevServiceConfig {
    }

    record TestFirebase(
            DevService devservice,
            FirebaseDevServiceConfig.Auth auth,
            FirebaseDevServiceConfig.Hosting hosting) implements FirebaseDevServiceConfig.Firebase {
    }

    record TestFirebaseDevService(
            boolean preferFirebaseDevServices,
            String imageName,
            Optional<Integer> dockerUser,
            Optional<Integer> dockerGroup,
            String firebaseVersion,
            Optional<String> token,
            Optional<String> customFirebaseJson,
            Optional<String> javaToolOptions,
            Optional<String> emulatorData,
            UI ui) implements FirebaseDevServiceConfig.Firebase.DevService {
    }

    record TestUI(
            boolean enabled,
            Optional<Integer> emulatorPort,
            Optional<Integer> loggingPort,
            Optional<Integer> hubPort) implements FirebaseDevServiceConfig.Firebase.DevService.UI {
    }

    record TestAuth(
            FirebaseDevServiceConfig.GenericDevService devservice) implements FirebaseDevServiceConfig.Auth {
    }

    record TestDatabase(
            FirebaseDevServiceConfig.GenericDevService devservice) implements FirebaseDevServiceConfig.Database {
    }

    record TestFirestore(
            FirestoreDevService devservice) implements FirebaseDevServiceConfig.Firestore {
    }

    record TestFirestoreDevService(
            boolean enabled,
            Optional<Integer> emulatorPort,
            Optional<Integer> websocketPort,
            Optional<String> rulesFile,
            Optional<String> indexesFile) implements FirebaseDevServiceConfig.Firestore.FirestoreDevService {
    }

    record TestFunctions(
            FirebaseDevServiceConfig.GenericDevService devservice) implements FirebaseDevServiceConfig.Functions {
    }

    record TestHosting(
            FirebaseDevServiceConfig.GenericDevService devservice,
            Optional<String> hostingPath) implements FirebaseDevServiceConfig.Hosting {
    }

    record TestPubSub(
            FirebaseDevServiceConfig.GenericDevService devservice) implements FirebaseDevServiceConfig.PubSub {
    }

    record TestStorage(
            FirebaseDevServiceConfig.Storage.StorageDevService devservice) implements FirebaseDevServiceConfig.Storage {
    }

    record TestStorageDevService(
            boolean enabled,
            Optional<Integer> emulatorPort,
            Optional<String> rulesFile) implements FirebaseDevServiceConfig.Storage.StorageDevService {

    }

    record TestGenericDevService(
            boolean enabled,
            Optional<Integer> emulatorPort) implements FirebaseDevServiceConfig.GenericDevService {
    }

}
