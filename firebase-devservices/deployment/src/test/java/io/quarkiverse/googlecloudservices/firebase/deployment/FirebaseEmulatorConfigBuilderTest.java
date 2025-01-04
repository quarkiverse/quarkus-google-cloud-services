package io.quarkiverse.googlecloudservices.firebase.deployment;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkiverse.googlecloudservices.firebase.deployment.testcontainers.FirebaseEmulatorContainer;

class FirebaseEmulatorConfigBuilderTest {

    private FirebaseEmulatorConfigBuilder configBuilder;

    @BeforeEach
    void setUp() {
        var projectConfig = new TestProjectConfig(
                Optional.of("my-project-id"));
        var config = new TestFirebaseDevServiceConfig(
                new TestFirebase(
                        true,
                        new TestFirebaseEmulator(
                                "11.0.0",
                                new TestDocker(
                                        "node:21-alpine",
                                        Optional.of(1001),
                                        Optional.of(1002),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.of(false),
                                        Optional.of(true)),
                                new TestCli(
                                        Optional.of("MY_TOKEN"),
                                        Optional.of("-Xmx"),
                                        Optional.of("data"),
                                        Optional.of(FirebaseEmulatorContainer.ImportExport.EXPORT_ONLY),
                                        Optional.of(Set.of("webframeworks")),
                                        Optional.of(true)),
                                Optional.empty(),
                                new TestUI(
                                        true,
                                        Optional.of(6000),
                                        Optional.of(6001),
                                        Optional.of(6002))),
                        new TestGenericDevService(true, Optional.of(6003)),
                        new TestHosting(
                                true,
                                Optional.of(6004),
                                Optional.of("public")),
                        new TestGenericDevService(
                                false,
                                Optional.of(6005)),
                        new TestFirestoreDevService(
                                true,
                                Optional.of(6006),
                                Optional.of(6007),
                                Optional.of("firestore.rules"),
                                Optional.of("firestore.indexes.json"))),
                new TestGenericDevService(
                        true,
                        Optional.of(6008)),
                new TestGenericDevService(
                        true,
                        Optional.of(6009)),
                new TestStorageDevService(
                        true,
                        Optional.empty(),
                        Optional.of("storage.rules")));
        configBuilder = new FirebaseEmulatorConfigBuilder(projectConfig, config);
    }

    @Test
    void testBuild() {
        FirebaseEmulatorContainer.EmulatorConfig emulatorConfig = configBuilder.buildConfig();

        assertNotNull(emulatorConfig);

        assertEquals("node:21-alpine", emulatorConfig.dockerConfig().imageName());
        assertEquals(1001, emulatorConfig.dockerConfig().userId().orElse(null));
        assertEquals(1002, emulatorConfig.dockerConfig().groupId().orElse(null));
        assertFalse(emulatorConfig.dockerConfig().followStdOut());
        assertTrue(emulatorConfig.dockerConfig().followStdErr());

        assertEquals("11.0.0", emulatorConfig.firebaseVersion());

        assertEquals("my-project-id", emulatorConfig.cliArguments().projectId().orElse(null));
        assertEquals("MY_TOKEN", emulatorConfig.cliArguments().token().orElse(null));
        assertEquals("-Xmx", emulatorConfig.cliArguments().javaToolOptions().orElse(null));
        assertPathEndsWith("data", emulatorConfig.cliArguments().emulatorData().orElse(null));
        assertEquals(Set.of("webframeworks"), emulatorConfig.cliArguments().experiments().orElse(null));
        assertEquals(FirebaseEmulatorContainer.ImportExport.EXPORT_ONLY, emulatorConfig.cliArguments().importExport());
        assertTrue(emulatorConfig.cliArguments().debug());

        assertTrue(emulatorConfig.customFirebaseJson().isEmpty());

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
        FirebaseEmulatorContainer.EmulatorConfig emulatorConfig = configBuilder.buildConfig();

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
    record TestProjectConfig(
            Optional<String> projectId) implements FirebaseDevServiceProjectConfig {
    }

    record TestFirebaseDevServiceConfig(
            FirebaseDevServiceConfig.Firebase firebase,
            FirebaseDevServiceConfig.GenericDevService functions,
            FirebaseDevServiceConfig.GenericDevService pubsub,
            FirebaseDevServiceConfig.StorageDevService storage) implements FirebaseDevServiceConfig {
    }

    record TestFirebase(
            boolean preferFirebaseDevServices,
            Emulator emulator,
            FirebaseDevServiceConfig.GenericDevService auth,
            FirebaseDevServiceConfig.Firebase.HostingDevService hosting,
            FirebaseDevServiceConfig.GenericDevService database,
            FirebaseDevServiceConfig.Firebase.FirestoreDevService firestore) implements FirebaseDevServiceConfig.Firebase {

    }

    record TestFirebaseEmulator(
            String firebaseVersion,
            FirebaseDevServiceConfig.Firebase.Emulator.Docker docker,
            FirebaseDevServiceConfig.Firebase.Emulator.Cli cli,
            Optional<String> customFirebaseJson,
            UI ui) implements FirebaseDevServiceConfig.Firebase.Emulator {
    }

    record TestDocker(
            String imageName,
            Optional<Integer> dockerUser,
            Optional<Integer> dockerGroup,
            Optional<String> dockerUserEnv,
            Optional<String> dockerGroupEnv,
            Optional<Boolean> followStdOut,
            Optional<Boolean> followStdErr) implements FirebaseDevServiceConfig.Firebase.Emulator.Docker {
    }

    record TestCli(
            Optional<String> token,
            Optional<String> javaToolOptions,
            Optional<String> emulatorData,
            Optional<FirebaseEmulatorContainer.ImportExport> importExport,
            Optional<Set<String>> experiments,
            Optional<Boolean> debug) implements FirebaseDevServiceConfig.Firebase.Emulator.Cli {
    }

    record TestUI(
            boolean enabled,
            Optional<Integer> emulatorPort,
            Optional<Integer> loggingPort,
            Optional<Integer> hubPort) implements FirebaseDevServiceConfig.Firebase.Emulator.UI {
    }

    record TestFirestoreDevService(
            boolean enabled,
            Optional<Integer> emulatorPort,
            Optional<Integer> websocketPort,
            Optional<String> rulesFile,
            Optional<String> indexesFile) implements FirebaseDevServiceConfig.Firebase.FirestoreDevService {
    }

    record TestHosting(
            boolean enabled,
            Optional<Integer> emulatorPort,
            Optional<String> hostingPath) implements FirebaseDevServiceConfig.Firebase.HostingDevService {
    }

    record TestStorageDevService(
            boolean enabled,
            Optional<Integer> emulatorPort,
            Optional<String> rulesFile) implements FirebaseDevServiceConfig.StorageDevService {

    }

    record TestGenericDevService(
            boolean enabled,
            Optional<Integer> emulatorPort) implements FirebaseDevServiceConfig.GenericDevService {
    }

}
