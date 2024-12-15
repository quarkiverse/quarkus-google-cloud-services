package io.quarkiverse.googlecloudservices.firebase.deployment.testcontainers;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Consumer;

import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkiverse.googlecloudservices.firebase.deployment.firebase.json.*;

/**
 * This class is responsible to generate the Firebase.json file which controls the emulators.
 */
public class FirebaseJsonBuilder {

    private static final String ALL_IP = "0.0.0.0";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FirebaseEmulatorContainer.EmulatorConfig emulatorConfig;
    private final FirebaseConfig root;

    public FirebaseJsonBuilder(FirebaseEmulatorContainer.EmulatorConfig emulatorConfig) {
        this.emulatorConfig = emulatorConfig;
        this.root = new FirebaseConfig();
    }

    public String buildFirebaseConfig() throws IOException {
        generateFirebaseConfig();

        StringWriter writer = new StringWriter();
        objectMapper.writeValue(writer, root);
        return writer.toString();
    }

    private void generateFirebaseConfig() {
        //        private Object database;
        //        private Object dataconnect;
        configureEmulator();
        //        private ExtensionsConfig extensions;
        configureFirestore();
        //        private Object functions;
        //        private Object hosting;
        //        private Remoteconfig remoteconfig;
        configureStorage();
    }

    private void configureEmulator() {
        var emulators = new Emulators();
        root.setEmulators(emulators);

        withEmulator(FirebaseEmulatorContainer.Emulator.AUTHENTICATION, (port) -> {
            var auth = new Auth();
            emulators.setAuth(auth);
            auth.setHost(ALL_IP);
            auth.setPort(port);
        });

        withEmulator(FirebaseEmulatorContainer.Emulator.REALTIME_DATABASE, (port) -> {
            var database = new Database();
            emulators.setDatabase(database);
            database.setHost(ALL_IP);
            database.setPort(port);
        });

        withEmulator(FirebaseEmulatorContainer.Emulator.CLOUD_FIRESTORE, (port) -> {
            var firestore = new Firestore();
            emulators.setFirestore(firestore);
            firestore.setHost(ALL_IP);
            firestore.setPort(port);
            withEmulator(FirebaseEmulatorContainer.Emulator.CLOUD_FIRESTORE_WS, firestore::setWebsocketPort);
        });

        withEmulator(FirebaseEmulatorContainer.Emulator.CLOUD_FUNCTIONS, (port) -> {
            var functions = new Functions();
            emulators.setFunctions(functions);
            functions.setHost(ALL_IP);
            functions.setPort(port);
        });

        withEmulator(FirebaseEmulatorContainer.Emulator.EVENT_ARC, (port) -> {
            var eventarc = new Eventarc();
            emulators.setEventarc(eventarc);
            eventarc.setHost(ALL_IP);
            eventarc.setPort(port);
        });

        withEmulator(FirebaseEmulatorContainer.Emulator.FIREBASE_HOSTING, (port) -> {
            var hosting = new Hosting();
            emulators.setHosting(hosting);
            hosting.setHost(ALL_IP);
            hosting.setPort(port);
        });

        withEmulator(FirebaseEmulatorContainer.Emulator.EMULATOR_HUB, (port) -> {
            var hub = new Hub();
            emulators.setHub(hub);
            hub.setHost(ALL_IP);
            hub.setPort(port);
        });

        withEmulator(FirebaseEmulatorContainer.Emulator.LOGGING, (port) -> {
            var logging = new Logging();
            emulators.setLogging(logging);
            logging.setHost(ALL_IP);
            logging.setPort(port);
        });

        withEmulator(FirebaseEmulatorContainer.Emulator.PUB_SUB, (port) -> {
            var pubSub = new Pubsub();
            emulators.setPubsub(pubSub);
            pubSub.setHost(ALL_IP);
            pubSub.setPort(port);
        });

        withEmulator(FirebaseEmulatorContainer.Emulator.CLOUD_STORAGE, (port) -> {
            var storage = new Storage();
            emulators.setStorage(storage);
            storage.setHost(ALL_IP);
            storage.setPort(port);
        });

        withEmulator(FirebaseEmulatorContainer.Emulator.EMULATOR_SUITE_UI, (port) -> {
            var ui = new Ui();
            emulators.setUi(ui);
            ui.setHost(ALL_IP);
            ui.setPort(port);
        });

        // Missing emulators
        //        private Apphosting apphosting;
        //        private Dataconnect dataconnect;
        //        private Extensions extensions;
        //        private Boolean singleProjectMode;
        //        private Tasks tasks;
    }

    private void withEmulator(FirebaseEmulatorContainer.Emulator emulator, Consumer<Integer> handler) {
        if (isEmulatorEnabled(emulator)) {
            var exposedPort = emulatorConfig.firebaseConfig().services().get(emulator);
            var port = Optional.ofNullable(exposedPort.fixedPort())
                    .orElse(emulator.internalPort);

            handler.accept(port);
        }
    }

    private void configureFirestore() {
        if (isEmulatorEnabled(FirebaseEmulatorContainer.Emulator.CLOUD_FIRESTORE)) {
            var firestore = new HashMap<String, String>(); // Generated sources can't handle anyOf yet
            root.setFirestore(firestore);

            emulatorConfig.firebaseConfig().firestoreConfig().rulesFile().ifPresent(rules -> {
                var rulesFile = fileRelativeToCustomJsonOrDefault(rules, "firestore.rules");
                firestore.put("rules", rulesFile);
            });

            emulatorConfig.firebaseConfig().firestoreConfig().indexesFile().ifPresent(index -> {
                var indexFile = fileRelativeToCustomJsonOrDefault(index, "firestore.indexes.json");
                firestore.put("indexes", indexFile);
            });
        }
    }

    private void configureStorage() {
        if (isEmulatorEnabled(FirebaseEmulatorContainer.Emulator.CLOUD_STORAGE)) {
            emulatorConfig.firebaseConfig().storageConfig().rulesFile().ifPresent(rules -> {
                var storage = new HashMap<String, String>(); // Generated sources can't handle anyOf yet
                root.setStorage(storage);

                var rulesFile = fileRelativeToCustomJsonOrDefault(rules, "storage.rules");
                storage.put("rules", rulesFile);
            });
        }
    }

    private String fileRelativeToCustomJsonOrDefault(Path otherFile, String defaultFile) {
        return emulatorConfig.customFirebaseJson()
                .map(path -> relativePath(path, otherFile))
                .orElse(defaultFile);
    }

    private String relativePath(Path firebaseJson, Path otherFile) {
        return firebaseJson.getParent().relativize(otherFile).toString();
    }

    private boolean isEmulatorEnabled(FirebaseEmulatorContainer.Emulator emulator) {
        return this.emulatorConfig.firebaseConfig().services().containsKey(emulator);
    }

}
