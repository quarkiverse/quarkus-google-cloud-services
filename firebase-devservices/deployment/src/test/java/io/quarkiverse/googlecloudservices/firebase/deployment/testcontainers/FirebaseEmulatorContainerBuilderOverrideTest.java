package io.quarkiverse.googlecloudservices.firebase.deployment.testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Test for {@link FirebaseEmulatorContainer.Builder#overrideHostingPath(Path)} and
 * {@link FirebaseEmulatorContainer.Builder#overrideFunctionsPath(Path)} -- the mechanism that lets devservices
 * config win over a custom firebase.json's own hosting/functions path. See
 * {@code FirebaseEmulatorConfigBuilderTest} for the config-layer wiring of that same mechanism, and
 * {@link FirebaseEmulatorContainerCustomConfigTest} for the Docker-backed test that starts a real container from
 * this same fixture file without any override applied.
 */
class FirebaseEmulatorContainerBuilderOverrideTest {

    private static final File CUSTOM_FIREBASE_JSON = new File("src/test/firebase.json");

    @Test
    void overrideHostingPathReplacesOnlyTheHostingDirectory() throws IOException {
        var builder = FirebaseEmulatorContainer.builder()
                .readFromFirebaseJson(CUSTOM_FIREBASE_JSON.toPath());

        var before = builder.buildConfig().firebaseConfig();
        builder.overrideHostingPath(Path.of("some-other-hosting-dir"));
        var after = builder.buildConfig().firebaseConfig();

        assertEquals("some-other-hosting-dir", after.hostingConfig().hostingOverride().orElseThrow().toString());
        // hostingContentDir -- firebase.json's own declared, always-relative hosting source, which the
        // container side relies on -- must survive the override untouched.
        assertEquals(before.hostingConfig().hostingContentDir(), after.hostingConfig().hostingContentDir());
        // Everything else must survive the override untouched.
        assertEquals(before.functionsConfig(), after.functionsConfig());
        assertEquals(before.storageConfig(), after.storageConfig());
        assertEquals(before.firestoreConfig(), after.firestoreConfig());
        assertEquals(before.services(), after.services());
    }

    @Test
    void overrideFunctionsPathReplacesOnlyTheFunctionsDirectory() throws IOException {
        var builder = FirebaseEmulatorContainer.builder()
                .readFromFirebaseJson(CUSTOM_FIREBASE_JSON.toPath());

        var before = builder.buildConfig().firebaseConfig();
        builder.overrideFunctionsPath(Path.of("some-other-functions-dir"));
        var after = builder.buildConfig().firebaseConfig();

        assertEquals("some-other-functions-dir", after.functionsConfig().functionsPath().orElseThrow().toString());
        // Everything else must survive the override untouched.
        assertEquals(before.hostingConfig(), after.hostingConfig());
        assertEquals(before.storageConfig(), after.storageConfig());
        assertEquals(before.firestoreConfig(), after.firestoreConfig());
        assertEquals(before.services(), after.services());
    }

    @Test
    void overrideHostingPathBeforeAnyConfigIsLoadedThrows() {
        var builder = FirebaseEmulatorContainer.builder();

        assertThrows(IllegalStateException.class, () -> builder.overrideHostingPath(Path.of("hosting")));
    }

    @Test
    void overrideFunctionsPathBeforeAnyConfigIsLoadedThrows() {
        var builder = FirebaseEmulatorContainer.builder();

        assertThrows(IllegalStateException.class, () -> builder.overrideFunctionsPath(Path.of("functions")));
    }
}
