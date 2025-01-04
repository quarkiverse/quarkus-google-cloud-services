package io.quarkiverse.googlecloudservices.firebase.deployment.testcontainers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkiverse.googlecloudservices.firebase.deployment.testcontainers.json.Emulators;
import io.quarkiverse.googlecloudservices.firebase.deployment.testcontainers.json.FirebaseConfig;

/**
 * Reader for the firebase.json file to convert it to the
 * {@link io.quarkiverse.googlecloudservices.firebase.deployment.testcontainers.FirebaseEmulatorContainer.FirebaseConfig}
 */
class CustomFirebaseConfigReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomFirebaseConfigReader.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Read the firebase config from a firebase.json file
     *
     * @param customFirebaseJson The path to the file
     * @return The configuration
     * @throws IOException In case the file could not be read
     */
    public FirebaseEmulatorContainer.FirebaseConfig readFromFirebase(Path customFirebaseJson) throws IOException {
        var root = readCustomFirebaseJson(customFirebaseJson);

        return new FirebaseEmulatorContainer.FirebaseConfig(
                readHosting(root.getHosting(), customFirebaseJson),
                readStorage(root.getStorage(), customFirebaseJson),
                readFirestore(root.getFirestore(), customFirebaseJson),
                readFunctions(root.getFunctions(), customFirebaseJson),
                readEmulators(root.getEmulators()));
    }

    private record EmulatorMergeStrategy<T>(
            FirebaseEmulatorContainer.Emulator emulator,
            Supplier<T> configObjectSupplier,
            Function<T, Supplier<Integer>> portSupplier) {
    }

    private Map<FirebaseEmulatorContainer.Emulator, FirebaseEmulatorContainer.ExposedPort> readEmulators(Emulators em) {
        var mergeStrategies = new EmulatorMergeStrategy<?>[] {
                new EmulatorMergeStrategy<>(
                        FirebaseEmulatorContainer.Emulator.AUTHENTICATION,
                        em::getAuth,
                        a -> a::getPort),
                new EmulatorMergeStrategy<>(
                        FirebaseEmulatorContainer.Emulator.EMULATOR_SUITE_UI,
                        em::getUi,
                        u -> u::getPort),
                new EmulatorMergeStrategy<>(
                        FirebaseEmulatorContainer.Emulator.EMULATOR_HUB,
                        em::getHub,
                        h -> h::getPort),
                new EmulatorMergeStrategy<>(
                        FirebaseEmulatorContainer.Emulator.LOGGING,
                        em::getLogging,
                        l -> l::getPort),
                new EmulatorMergeStrategy<>(
                        FirebaseEmulatorContainer.Emulator.CLOUD_FUNCTIONS,
                        em::getFunctions,
                        f -> f::getPort),
                new EmulatorMergeStrategy<>(
                        FirebaseEmulatorContainer.Emulator.EVENT_ARC,
                        em::getEventarc,
                        e -> e::getPort),
                new EmulatorMergeStrategy<>(
                        FirebaseEmulatorContainer.Emulator.REALTIME_DATABASE,
                        em::getDatabase,
                        d -> d::getPort),
                new EmulatorMergeStrategy<>(
                        FirebaseEmulatorContainer.Emulator.CLOUD_FIRESTORE,
                        em::getFirestore,
                        d -> d::getPort),
                new EmulatorMergeStrategy<>(
                        FirebaseEmulatorContainer.Emulator.CLOUD_STORAGE,
                        em::getStorage,
                        s -> s::getPort),
                new EmulatorMergeStrategy<>(
                        FirebaseEmulatorContainer.Emulator.FIREBASE_HOSTING,
                        em::getHosting,
                        h -> h::getPort),
                new EmulatorMergeStrategy<>(
                        FirebaseEmulatorContainer.Emulator.PUB_SUB,
                        em::getPubsub,
                        h -> h::getPort)
        };

        var map = Arrays.stream(mergeStrategies)
                .map(this::mergeEmulator)
                .filter(e -> !Objects.isNull(e))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (em.getFirestore() != null && em.getFirestore().getWebsocketPort() != null) {
            map = new HashMap<>(map);

            map.put(
                    FirebaseEmulatorContainer.Emulator.CLOUD_FIRESTORE_WS,
                    new FirebaseEmulatorContainer.ExposedPort(em.getFirestore().getWebsocketPort()));

            map = Map.copyOf(map);
        }

        LOGGER.debug("Found the following emulators configured in firebase.json {}", map.keySet());

        return map;
    }

    private <T> Map.Entry<FirebaseEmulatorContainer.Emulator, FirebaseEmulatorContainer.ExposedPort> mergeEmulator(
            EmulatorMergeStrategy<T> emulatorMergeStrategy) {

        var configObject = emulatorMergeStrategy.configObjectSupplier.get();
        if (configObject != null) {
            var port = emulatorMergeStrategy.portSupplier.apply(configObject).get();
            return Map.entry(emulatorMergeStrategy.emulator, new FirebaseEmulatorContainer.ExposedPort(port));
        } else {
            return null;
        }
    }

    private FirebaseEmulatorContainer.FirestoreConfig readFirestore(Object firestore, Path customFirebaseJson) {
        if (firestore instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> firestoreMap = (Map<String, String>) firestore;

            var rulesFile = Optional
                    .ofNullable(firestoreMap.get("rules"))
                    .map(f -> this.resolvePath(f, customFirebaseJson));
            var indexesFile = Optional
                    .ofNullable(firestoreMap.get("indexes"))
                    .map(f -> this.resolvePath(f, customFirebaseJson));

            LOGGER.debug("Firestore configured with rules file {}", rulesFile);
            LOGGER.debug("Firestore configured with indexes file {}", indexesFile);

            return new FirebaseEmulatorContainer.FirestoreConfig(
                    rulesFile,
                    indexesFile);
        } else {
            return FirebaseEmulatorContainer.FirestoreConfig.DEFAULT;
        }
    }

    private FirebaseEmulatorContainer.HostingConfig readHosting(Object hosting, Path customFirebaseJson) {
        if (hosting instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> hostingMap = (Map<String, String>) hosting;

            var publicDir = Optional
                    .ofNullable(hostingMap.get("public"))
                    .or(() -> Optional.ofNullable(hostingMap.get("source")))
                    .map(f -> this.resolvePath(f, customFirebaseJson));

            LOGGER.debug("Hosting configured with public directory {}", publicDir);

            return new FirebaseEmulatorContainer.HostingConfig(
                    publicDir);
        } else {
            return FirebaseEmulatorContainer.HostingConfig.DEFAULT;
        }
    }

    private FirebaseEmulatorContainer.StorageConfig readStorage(Object storage, Path customFirebaseJson) {
        if (storage instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> storageMap = (Map<String, String>) storage;

            var rulesFile = Optional
                    .ofNullable(storageMap.get("rules"))
                    .map(f -> this.resolvePath(f, customFirebaseJson));

            LOGGER.debug("Storage configured with rules file {}", rulesFile);

            return new FirebaseEmulatorContainer.StorageConfig(
                    rulesFile);
        } else {
            return FirebaseEmulatorContainer.StorageConfig.DEFAULT;
        }
    }

    private FirebaseEmulatorContainer.FunctionsConfig readFunctions(Object functions, Path customFirebaseJson) {
        if (functions instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> functionsMap = (Map<String, Object>) functions;

            var functionsPath = Optional
                    .ofNullable(functionsMap.get("source"))
                    .map(String.class::cast)
                    .map(f -> this.resolvePath(f, customFirebaseJson));

            var ignores = Optional
                    .ofNullable(functionsMap.get("ignores"))
                    .map(String[].class::cast)
                    .orElse(new String[0]);

            LOGGER.debug("Functions will be read from source directory {}", functionsPath);
            LOGGER.debug("Functions configured with ignore file {}", (Object) ignores);

            return new FirebaseEmulatorContainer.FunctionsConfig(
                    functionsPath,
                    ignores);
        } else {
            return FirebaseEmulatorContainer.FunctionsConfig.DEFAULT;
        }
    }

    private Path resolvePath(String filename, Path customFirebaseJson) {
        File firebaseJson = customFirebaseJson.toFile();
        File firebaseDir = firebaseJson.getParentFile();
        return new File(firebaseDir, filename).toPath();
    }

    private FirebaseConfig readCustomFirebaseJson(Path customFirebaseJson) throws IOException {
        var customFirebaseFile = customFirebaseJson.toFile();
        var customFirebaseStream = new BufferedInputStream(new FileInputStream(customFirebaseFile));

        return objectMapper.readerFor(FirebaseConfig.class)
                .readValue(customFirebaseStream);
    }

}
