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
                                Optional.of("11.0.0"),
                                new TestDocker(
                                        "node:21-alpine",
                                        Optional.of(1001),
                                        Optional.of(1002),
                                        Optional.empty(),
                                        Optional.empty(),
                                        false,
                                        Optional.of(false),
                                        Optional.of(true),
                                        Map.of("TEST", "TestMe")),
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
                                        Optional.of(6002)),
                                true),
                        new TestGenericDevService(true, Optional.of(6003)),
                        new TestHosting(
                                true,
                                Optional.of(6004),
                                Optional.of("public"),
                                new TestVite(Optional.empty())),
                        new TestGenericDevService(
                                false,
                                Optional.of(6005)),
                        new TestFirestoreDevService(
                                true,
                                Optional.of(6006),
                                Optional.of(6007),
                                Optional.of("firestore.rules"),
                                Optional.of("firestore.indexes.json"))),
                new TestFunctionsDevService(
                        true,
                        Optional.of(6008),
                        Optional.empty()),
                new TestGenericDevService(
                        true,
                        Optional.of(6009)),
                new TestStorageDevService(
                        true,
                        Optional.empty(),
                        Optional.of("storage.rules")));
        configBuilder = new FirebaseEmulatorConfigBuilder(projectConfig, config, true);
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
        assertTrue(emulatorConfig.dockerConfig().useSharedNetwork());
        assertEquals("TestMe", emulatorConfig.dockerConfig().envVars().get("TEST"));

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

    // src/test/firebase.json (used by the Testcontainers-backed
    // testcontainers.FirebaseEmulatorContainerCustomConfigTest too) sets hosting.public="hosting" and
    // functions.source="functions" -- these tests only build the config (no container start), so they stay
    // fast/Docker-free while still exercising the real file the plain-JSON parsing reads.
    private static final String CUSTOM_FIREBASE_JSON = "src/test/firebase.json";

    @Test
    void testCustomFirebaseJsonAppliesHostingAndFunctionsPathOverride() {
        var configBuilder = customFirebaseJsonConfigBuilder(
                Optional.of("overridden-hosting"),
                Optional.of("overridden-functions"));

        var firebaseConfig = configBuilder.buildConfig().firebaseConfig();

        assertPathEndsWith("overridden-hosting", firebaseConfig.hostingConfig().hostingOverride().orElse(null));
        assertPathEndsWith("overridden-functions", firebaseConfig.functionsConfig().functionsPath().orElse(null));
        assertPathEndsWith("hosting", firebaseConfig.hostingConfig().hostingContentDir().orElse(null));
        // Everything else that came from the custom firebase.json must survive the override untouched.
        assertPathEndsWith("firestore.rules", firebaseConfig.firestoreConfig().rulesFile().orElse(null));
        assertPathEndsWith("storage.rules", firebaseConfig.storageConfig().rulesFile().orElse(null));
    }

    @Test
    void testCustomFirebaseJsonWithoutOverrideKeepsOriginalPaths() {
        var configBuilder = customFirebaseJsonConfigBuilder(Optional.empty(), Optional.empty());

        var firebaseConfig = configBuilder.buildConfig().firebaseConfig();

        assertPathEndsWith("hosting", firebaseConfig.hostingConfig().hostingContentDir().orElse(null));
        assertPathEndsWith("functions", firebaseConfig.functionsConfig().functionsPath().orElse(null));
    }

    private FirebaseEmulatorConfigBuilder customFirebaseJsonConfigBuilder(
            Optional<String> hostingPathOverride,
            Optional<String> functionsPathOverride) {
        var projectConfig = new TestProjectConfig(Optional.of("my-project-id"));
        var config = new TestFirebaseDevServiceConfig(
                new TestFirebase(
                        true,
                        new TestFirebaseEmulator(
                                Optional.empty(),
                                new TestDocker(
                                        "node:21-alpine",
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        true,
                                        Optional.empty(),
                                        Optional.empty(),
                                        Map.of()),
                                new TestCli(
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty()),
                                Optional.of(CUSTOM_FIREBASE_JSON),
                                new TestUI(false, Optional.empty(), Optional.empty(), Optional.empty()),
                                true),
                        new TestGenericDevService(false, Optional.empty()),
                        new TestHosting(
                                false,
                                Optional.empty(),
                                hostingPathOverride,
                                new TestVite(Optional.empty())),
                        new TestGenericDevService(false, Optional.empty()),
                        new TestFirestoreDevService(
                                false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())),
                new TestFunctionsDevService(false, Optional.empty(), functionsPathOverride),
                new TestGenericDevService(false, Optional.empty()),
                new TestStorageDevService(false, Optional.empty(), Optional.empty()));

        return new FirebaseEmulatorConfigBuilder(projectConfig, config, true);
    }

    // Record implementations for interfaces
    record TestProjectConfig(
            Optional<String> projectId) implements FirebaseDevServiceProjectConfig {
    }

    record TestFirebaseDevServiceConfig(
            FirebaseDevServiceConfig.Firebase firebase,
            FirebaseDevServiceConfig.FunctionsDevService functions,
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
            Optional<String> firebaseVersion,
            FirebaseDevServiceConfig.Firebase.Emulator.Docker docker,
            FirebaseDevServiceConfig.Firebase.Emulator.Cli cli,
            Optional<String> customFirebaseJson,
            UI ui,
            boolean exposeToCompanionContainers) implements FirebaseDevServiceConfig.Firebase.Emulator {
    }

    record TestDocker(
            String imageName,
            Optional<Integer> dockerUser,
            Optional<Integer> dockerGroup,
            Optional<String> dockerUserEnv,
            Optional<String> dockerGroupEnv,
            boolean autoDetectUserAndGroup,
            Optional<Boolean> followStdOut,
            Optional<Boolean> followStdErr,
            Map<String, String> envVars) implements FirebaseDevServiceConfig.Firebase.Emulator.Docker {
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
            Optional<String> hostingPath,
            FirebaseDevServiceConfig.Firebase.HostingDevService.Vite vite)
            implements
                FirebaseDevServiceConfig.Firebase.HostingDevService {
    }

    record TestVite(
            Optional<Integer> hmrPort) implements FirebaseDevServiceConfig.Firebase.HostingDevService.Vite {
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

    record TestFunctionsDevService(
            boolean enabled,
            Optional<Integer> emulatorPort,
            Optional<String> functionsPath) implements FirebaseDevServiceConfig.FunctionsDevService {
    }

}
