package io.quarkiverse.googlecloudservices.firebase.deployment;

import java.io.File;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class translates the Quarkus Firebase extension configuration to the {@link FirebaseEmulatorContainer}
 * configuration.
 */
public class FirebaseEmulatorConfigBuilder {

    private final FirebaseDevServiceConfig config;

    public FirebaseEmulatorConfigBuilder(FirebaseDevServiceConfig config) {
        this.config = config;
    }

    public FirebaseEmulatorContainer.EmulatorConfig build() {
        var devService = config.firebase().devservice();

        return new FirebaseEmulatorContainer.EmulatorConfig(
                devService.imageName(),
                devService.firebaseVersion(),
                config.projectId(),
                devService.token(),
                devService.customFirebaseJson().map(File::new).map(File::toPath),
                devService.javaToolOptions(),
                devService.emulatorData().map(File::new).map(File::toPath),
                config.firebase().hosting().hostingPath().map(File::new).map(File::toPath),
                exposedEmulators(devServices(config)));
    }

    public static Map<FirebaseEmulatorContainer.Emulator, FirebaseDevServiceConfig.GenericDevService> devServices(
            FirebaseDevServiceConfig config) {
        return Map.of(
                FirebaseEmulatorContainer.Emulator.AUTHENTICATION, config.firebase().auth().devservice(),
                FirebaseEmulatorContainer.Emulator.EMULATOR_SUITE_UI, config.firebase().devservice().ui(),
                FirebaseEmulatorContainer.Emulator.REALTIME_DATABASE, config.database().devservice(),
                FirebaseEmulatorContainer.Emulator.CLOUD_FIRESTORE, config.firestore().devservice(),
                FirebaseEmulatorContainer.Emulator.CLOUD_FUNCTIONS, config.functions().devservice(),
                FirebaseEmulatorContainer.Emulator.CLOUD_STORAGE, config.storage().devservice(),
                FirebaseEmulatorContainer.Emulator.FIREBASE_HOSTING, config.firebase().hosting().devservice(),
                FirebaseEmulatorContainer.Emulator.PUB_SUB, config.pubsub().devservice());
    }

    private Map<FirebaseEmulatorContainer.Emulator, FirebaseEmulatorContainer.ExposedPort> exposedEmulators(
            Map<FirebaseEmulatorContainer.Emulator, FirebaseDevServiceConfig.GenericDevService> devServices) {
        var emulators = devServices
                .entrySet()
                .stream()
                .filter(e -> e.getValue().enabled())
                .map(e -> Map.entry(e.getKey(), portForService(e.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var uiService = (FirebaseDevServiceConfig.Firebase.DevService.UI) devServices
                .get(FirebaseEmulatorContainer.Emulator.EMULATOR_SUITE_UI);

        uiService.hubPort().ifPresent(port -> emulators.put(
                FirebaseEmulatorContainer.Emulator.EMULATOR_HUB,
                new FirebaseEmulatorContainer.ExposedPort(port)));

        uiService.loggingPort().ifPresent(port -> emulators.put(
                FirebaseEmulatorContainer.Emulator.LOGGING,
                new FirebaseEmulatorContainer.ExposedPort(port)));

        var firestoreService = (FirebaseDevServiceConfig.Firestore.FirestoreDevService) devServices
                .get(FirebaseEmulatorContainer.Emulator.CLOUD_FIRESTORE);

        firestoreService.websocketPort().ifPresent(port -> emulators.put(
                FirebaseEmulatorContainer.Emulator.CLOUD_FIRESTORE_WS,
                new FirebaseEmulatorContainer.ExposedPort(port)));

        // TODO: Event arc?

        return emulators;
    }

    private static FirebaseEmulatorContainer.ExposedPort portForService(FirebaseDevServiceConfig.GenericDevService devService) {
        var port = devService.emulatorPort().orElse(null);
        return new FirebaseEmulatorContainer.ExposedPort(port);
    }

}
