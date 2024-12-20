package io.quarkiverse.googlecloudservices.firebase.deployment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import io.quarkiverse.googlecloudservices.firebase.deployment.testcontainers.FirebaseEmulatorContainer;

/**
 * This class translates the Quarkus Firebase extension configuration to the {@link FirebaseEmulatorContainer}
 * instance.
 */
public class FirebaseEmulatorConfigBuilder {

    private final FirebaseDevServiceProjectConfig projectConfig;
    private final FirebaseDevServiceConfig config;

    public static Map<FirebaseEmulatorContainer.Emulator, FirebaseDevServiceConfig.GenericDevService> devServices(
            FirebaseDevServiceConfig config) {
        return Map.of(
                FirebaseEmulatorContainer.Emulator.AUTHENTICATION, config.firebase().auth(),
                FirebaseEmulatorContainer.Emulator.EMULATOR_SUITE_UI, config.firebase().emulator().ui(),
                FirebaseEmulatorContainer.Emulator.CLOUD_FUNCTIONS, config.functions(),
                FirebaseEmulatorContainer.Emulator.REALTIME_DATABASE, config.firebase().database(),
                FirebaseEmulatorContainer.Emulator.CLOUD_FIRESTORE, config.firebase().firestore(),
                FirebaseEmulatorContainer.Emulator.CLOUD_STORAGE, config.storage(),
                FirebaseEmulatorContainer.Emulator.FIREBASE_HOSTING, config.firebase().hosting(),
                FirebaseEmulatorContainer.Emulator.PUB_SUB, config.pubsub());
    }

    public FirebaseEmulatorConfigBuilder(FirebaseDevServiceProjectConfig projectConfig, FirebaseDevServiceConfig config) {
        this.projectConfig = projectConfig;
        this.config = config;
    }

    public FirebaseEmulatorContainer build() {
        return configureBuilder().build();
    }

    FirebaseEmulatorContainer.EmulatorConfig buildConfig() {
        return configureBuilder().buildConfig();
    }

    private FirebaseEmulatorContainer.Builder configureBuilder() {
        var builder = FirebaseEmulatorContainer.builder();

        builder.withFirebaseVersion(config.firebase().emulator().firebaseVersion());

        handleDockerConfig(config.firebase().emulator().docker(), builder);
        handleCliConfig(config.firebase().emulator().cli(), builder);
        handleEmulators(builder);

        return builder;
    }

    private void handleDockerConfig(FirebaseDevServiceConfig.Firebase.Emulator.Docker docker,
            FirebaseEmulatorContainer.Builder builder) {
        var dockerConfig = builder.withDockerConfig();

        dockerConfig.withImage(docker.imageName());
        docker.dockerUser().ifPresent(dockerConfig::withUserId);
        docker.dockerGroup().ifPresent(dockerConfig::withGroupId);
        docker.dockerUserEnv().ifPresent(dockerConfig::withUserIdFromEnv);
        docker.dockerGroupEnv().ifPresent(dockerConfig::withGroupIdFromEnv);
        docker.followStdOut().ifPresent(dockerConfig::followStdOut);
        docker.followStdErr().ifPresent(dockerConfig::followStdErr);

        dockerConfig.done();
    }

    private void handleCliConfig(FirebaseDevServiceConfig.Firebase.Emulator.Cli cli,
            FirebaseEmulatorContainer.Builder builder) {
        var cliConfig = builder.withCliArguments();

        projectConfig.projectId().ifPresent(cliConfig::withProjectId);

        cli.token().ifPresent(cliConfig::withToken);
        cli.javaToolOptions().ifPresent(cliConfig::withJavaToolOptions);
        cli.emulatorData().map(FirebaseEmulatorConfigBuilder::asPath).ifPresent(cliConfig::withEmulatorData);
        cli.importExport().ifPresent(cliConfig::withImportExport);
        cli.debug().ifPresent(cliConfig::withDebug);

        cliConfig.done();
    }

    private void handleEmulators(FirebaseEmulatorContainer.Builder builder) {
        config.firebase().emulator().customFirebaseJson().ifPresentOrElse(
                (firebaseJson) -> configureCustomFirebaseJson(builder, firebaseJson),
                () -> configureEmulators(builder));
    }

    private void configureCustomFirebaseJson(FirebaseEmulatorContainer.Builder builder, String firebaseJson) {
        try {
            builder.readFromFirebaseJson(asPath(firebaseJson));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void configureEmulators(FirebaseEmulatorContainer.Builder builder) {
        var devServices = devServices(config);

        var noEmulatorsConfigured = devServices
                .entrySet()
                .stream()
                .filter(e -> e.getValue().enabled())
                // Emulator Suite UI is enabled by default, so ignore it.
                .filter(e -> !e.getKey().equals(FirebaseEmulatorContainer.Emulator.EMULATOR_SUITE_UI))
                .findAny()
                .isEmpty();

        // No emulators configured via configuration, we will fallback to the automatic detection of a firebase.json file.
        if (noEmulatorsConfigured) {
            return;
        }

        var firebaseConfigBuilder = builder.withFirebaseConfig();
        devServices
                .entrySet()
                .stream()
                .filter(e -> e.getValue().enabled())
                .forEach(e -> e.getValue().emulatorPort().ifPresentOrElse(
                        p -> firebaseConfigBuilder.withEmulatorOnFixedPort(e.getKey(), p),
                        () -> firebaseConfigBuilder.withEmulator(e.getKey())));

        config.firebase()
                .hosting()
                .hostingPath()
                .map(FirebaseEmulatorConfigBuilder::asPath)
                .ifPresent(firebaseConfigBuilder::withHostingPath);

        config.firebase()
                .firestore()
                .indexesFile()
                .map(FirebaseEmulatorConfigBuilder::asPath)
                .ifPresent(firebaseConfigBuilder::withFirestoreIndexes);

        config.firebase()
                .firestore()
                .rulesFile()
                .map(FirebaseEmulatorConfigBuilder::asPath)
                .ifPresent(firebaseConfigBuilder::withFirestoreRules);

        config.firebase()
                .firestore()
                .websocketPort()
                .ifPresent(p -> firebaseConfigBuilder
                        .withEmulatorOnFixedPort(FirebaseEmulatorContainer.Emulator.CLOUD_FIRESTORE_WS, p));

        config.firebase()
                .emulator()
                .ui()
                .hubPort()
                .ifPresent(
                        p -> firebaseConfigBuilder.withEmulatorOnFixedPort(FirebaseEmulatorContainer.Emulator.EMULATOR_HUB, p));

        config.firebase()
                .emulator()
                .ui()
                .loggingPort()
                .ifPresent(p -> firebaseConfigBuilder.withEmulatorOnFixedPort(FirebaseEmulatorContainer.Emulator.LOGGING, p));

        config.storage()
                .rulesFile()
                .map(FirebaseEmulatorConfigBuilder::asPath)
                .ifPresent(firebaseConfigBuilder::withStorageRules);

        firebaseConfigBuilder.done();
    }

    private static Path asPath(String path) {
        return new File(path).toPath();
    }

}
