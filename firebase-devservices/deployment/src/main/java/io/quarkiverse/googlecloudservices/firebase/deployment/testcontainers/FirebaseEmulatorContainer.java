package io.quarkiverse.googlecloudservices.firebase.deployment.testcontainers;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.images.builder.dockerfile.DockerfileBuilder;

/**
 * Testcontainers container to run Firebase emulators from Docker.
 */
public class FirebaseEmulatorContainer extends GenericContainer<FirebaseEmulatorContainer> {

    private static final Logger LOGGER = Logger.getLogger(FirebaseEmulatorContainer.class.getName());

    public static final String FIREBASE_ROOT = "/srv/firebase";
    public static final String FIREBASE_HOSTING_PATH = FIREBASE_ROOT + "/public";
    public static final String EMULATOR_DATA_PATH = FIREBASE_ROOT + "/data";
    public static final String EMULATOR_EXPORT_PATH = EMULATOR_DATA_PATH + "/emulator-data";

    /**
     * Set of possible emulators (or components/services).
     */
    public enum Emulator {
        /**
         * Firebase Auth emulator
         */
        AUTHENTICATION(
                9099,
                "auth",
                "auth"),
        /**
         * Emulator UI, not a real emulator, but allows exposing the UI on a predefined port
         */
        EMULATOR_SUITE_UI(
                4000,
                "ui",
                "ui"),
        /**
         * Emulator Hub API port
         */
        EMULATOR_HUB(
                4400,
                "hub",
                null),
        /**
         * Emulator UI Logging endpoint
         */
        LOGGING(
                4500,
                "logging",
                null),
        /**
         * CLoud functions emulator
         */
        CLOUD_FUNCTIONS(
                5001,
                "functions",
                "functions"),
        /**
         * Event Arc emulator
         */
        EVENT_ARC(
                9299,
                "eventarc",
                "eventarc"),
        /**
         * Realtime database emulator
         */
        REALTIME_DATABASE(
                9000,
                "database",
                "database"),
        /**
         * Firestore emulator
         */
        CLOUD_FIRESTORE(
                8080,
                "firestore",
                "firestore"),
        /**
         * Firestore websocket port, This emulator always need to be specified in conjunction with CLOUD_FIRESTORE.
         */
        CLOUD_FIRESTORE_WS(
                9150,
                null,
                null),
        /**
         * Cloud storage emulator
         */
        CLOUD_STORAGE(
                9199,
                "storage",
                "storage"),
        /**
         * Firebase hosting emulator
         */
        FIREBASE_HOSTING(
                5000,
                "hosting",
                "hosting"),
        /**
         * Pub/sub emulator
         */
        PUB_SUB(
                8085,
                "pubsub",
                "pubsub");

        public final int internalPort;
        public final String configProperty;
        public final String emulatorName;

        Emulator(int internalPort, String configProperty, String onlyArgument) {
            this.internalPort = internalPort;
            this.configProperty = configProperty;
            this.emulatorName = onlyArgument;
        }

    }

    /**
     * Record to hold an exposed port of an emulator.
     *
     * @param fixedPort The exposed port or null in case it is a random port
     */
    public record ExposedPort(Integer fixedPort) {

        public boolean isFixed() {
            return fixedPort != null;
        }
    }

    /**
     * The docker image configuration
     *
     * @param imageName The name of the docker image
     * @param userId The user id to run the docker image
     * @param groupId The group id to run the docker image
     */
    public record DockerConfig(
            String imageName,
            Optional<Integer> userId,
            Optional<Integer> groupId) {
    }

    /**
     * Firebase hosting configuration
     *
     * @param hostingContentDir The path to the directory containing the hosting content
     */
    public record HostingConfig(
            Optional<Path> hostingContentDir) {
    }

    /**
     * Cloud storage configuration
     *
     * @param rulesFile Cloud storage rules file
     */
    public record StorageConfig(
            Optional<Path> rulesFile) {
    }

    /**
     * Firestore configuration
     *
     * @param rulesFile The rules file
     * @param indexesFile The indexes file
     */
    public record FirestoreConfig(
            Optional<Path> rulesFile,
            Optional<Path> indexesFile) {
    }

    /**
     *
     * @param hostingConfig The firebase hosting configuration
     * @param storageConfig The storage configuration
     * @param firestoreConfig The firestore configuration
     * @param services The exposed services configuration
     */
    public record FirebaseConfig(
            HostingConfig hostingConfig,
            StorageConfig storageConfig,
            FirestoreConfig firestoreConfig,
            Map<Emulator, ExposedPort> services) {
    }

    /**
     * Describes the Firebase emulator configuration.
     *
     * @param dockerConfig The docker configuration
     * @param firebaseVersion The firebase version to use
     * @param projectId The project ID, needed when running with the auth emulator
     * @param token The Google Cloud CLI token to use for authentication. Needed for firebase hosting
     * @param customFirebaseJson The path to a custom firebase
     * @param javaToolOptions The options to pass to the java based emulators
     * @param emulatorData The path to the directory where to store the emulator data
     * @param firebaseConfig The firebase configuration
     */
    public record EmulatorConfig(
            DockerConfig dockerConfig,
            String firebaseVersion,
            Optional<String> projectId,
            Optional<String> token,
            Optional<Path> customFirebaseJson,
            Optional<String> javaToolOptions,
            Optional<Path> emulatorData,
            FirebaseConfig firebaseConfig) {
    }

    private final Map<Emulator, ExposedPort> services;

    /**
     * Creates a new Firebase Emulator container
     *
     * @param emulatorConfig The generic configuration of the firebase emulators
     */
    public FirebaseEmulatorContainer(EmulatorConfig emulatorConfig) {
        super(new FirebaseDockerBuilder(emulatorConfig).build());

        emulatorConfig.emulatorData().ifPresent(path -> {
            // https://firebase.google.com/docs/emulator-suite/install_and_configure#export_and_import_emulator_data
            // Mount the volume to the specified path
            this.withFileSystemBind(path.toString(), EMULATOR_DATA_PATH, BindMode.READ_WRITE);
        });

        emulatorConfig.firebaseConfig().hostingConfig().hostingContentDir().ifPresent(hostingPath -> {
            // Mount volume for static hosting content
            this.withFileSystemBind(hostingPath.toString(), FIREBASE_HOSTING_PATH, BindMode.READ_ONLY);
        });

        this.services = emulatorConfig.firebaseConfig().services;
    }

    private static class FirebaseDockerBuilder {

        private static final Map<Emulator, String> DOWNLOADABLE_EMULATORS = Map.of(
                Emulator.REALTIME_DATABASE, "database",
                Emulator.CLOUD_FIRESTORE, "firestore",
                Emulator.PUB_SUB, "pubsub",
                Emulator.CLOUD_STORAGE, "storage",
                Emulator.EMULATOR_SUITE_UI, "ui");

        private final ImageFromDockerfile result;

        private final EmulatorConfig emulatorConfig;
        private final Map<Emulator, ExposedPort> devServices;

        private DockerfileBuilder dockerBuilder;

        public FirebaseDockerBuilder(EmulatorConfig emulatorConfig) {
            this.devServices = emulatorConfig.firebaseConfig().services;
            this.emulatorConfig = emulatorConfig;

            this.result = new ImageFromDockerfile("localhost/testcontainers/firebase", false)
                    .withDockerfileFromBuilder(builder -> this.dockerBuilder = builder);
        }

        public ImageFromDockerfile build() {
            this.validateConfiguration();
            this.configureBaseImage();
            this.initialSetup();
            this.authenticateToFirebase();
            this.setupJavaToolOptions();
            this.setupUserAndGroup();
            this.downloadEmulators();
            this.addFirebaseJson();
            this.includeFirestoreFiles();
            this.includeStorageFiles();
            this.setupDataImportExport();
            this.setupHosting();
            this.runExecutable();

            return result;
        }

        private void validateConfiguration() {
            if (isEmulatorEnabled(Emulator.AUTHENTICATION) && emulatorConfig.projectId().isEmpty()) {
                throw new IllegalStateException("Can't create Firebase Auth emulator. Google Project id is required");
            }

            if (isEmulatorEnabled(Emulator.EMULATOR_SUITE_UI)) {
                if (!isEmulatorEnabled(Emulator.EMULATOR_HUB)) {
                    LOGGER.info(
                            "Firebase Emulator UI is enabled, but no Hub port is specified. You will not be able to use the Hub API ");
                }

                if (!isEmulatorEnabled(Emulator.LOGGING)) {
                    LOGGER.info(
                            "Firebase Emulator UI is enabled, but no Logging port is specified. You will not be able to see the logging ");
                }

                if (isEmulatorEnabled(Emulator.CLOUD_FIRESTORE)) {
                    if (!isEmulatorEnabled(Emulator.CLOUD_FIRESTORE_WS)) {
                        LOGGER.warn("Firebase Firestore Emulator and Emulator UI are enabled but no Firestore Websocket " +
                                "port is specified. You will not be able to use the Firestore UI");
                    }
                }
            }

            // TODO: Validate if a custom firebase.json is defined, that the hosts are defined as 0.0.0.0
        }

        private void configureBaseImage() {
            dockerBuilder.from(emulatorConfig.dockerConfig().imageName());
        }

        private void initialSetup() {
            dockerBuilder
                    .run("apk --no-cache add openjdk11-jre bash curl openssl gettext nano nginx sudo && " +
                            "npm cache clean --force && " +
                            "npm i -g firebase-tools@" + emulatorConfig.firebaseVersion() + " && " +
                            "deluser nginx && delgroup abuild && delgroup ping && " +
                            "mkdir -p " + FIREBASE_ROOT + " && " +
                            "mkdir -p " + FIREBASE_HOSTING_PATH + " && " +
                            "mkdir -p " + EMULATOR_DATA_PATH + " && " +
                            "mkdir -p " + EMULATOR_EXPORT_PATH + " && " +
                            "chmod 777 -R /srv/*");
        }

        private void downloadEmulators() {
            var cmd = DOWNLOADABLE_EMULATORS
                    .entrySet()
                    .stream()
                    .map(e -> downloadEmulatorCommand(e.getKey(), e.getValue()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(" && "));

            dockerBuilder.run(cmd);
        }

        private String downloadEmulatorCommand(Emulator emulator, String downloadId) {
            if (isEmulatorEnabled(emulator)) {
                return "firebase setup:emulators:" + downloadId;
            } else {
                return null;
            }
        }

        private void authenticateToFirebase() {
            emulatorConfig.token().ifPresent(token -> dockerBuilder.env("FIREBASE_TOKEN", token));
        }

        private void setupJavaToolOptions() {
            emulatorConfig.javaToolOptions().ifPresent(toolOptions -> dockerBuilder.env("JAVA_TOOL_OPTIONS", toolOptions));
        }

        private void addFirebaseJson() {
            dockerBuilder.workDir(FIREBASE_ROOT);

            emulatorConfig.customFirebaseJson().ifPresentOrElse(
                    this::includeCustomFirebaseJson,
                    this::generateFirebaseJson);

            this.dockerBuilder.add("firebase.json", FIREBASE_ROOT + "/firebase.json");
        }

        private void includeCustomFirebaseJson(Path customFilePath) {
            this.result.withFileFromPath(
                    "firebase.json",
                    customFilePath);
        }

        private void includeFirestoreFiles() {
            emulatorConfig.firebaseConfig().firestoreConfig.rulesFile.ifPresent(rulesFile -> {
                this.dockerBuilder.add("firestore.rules", FIREBASE_ROOT + "/firestore.rules");
                this.result.withFileFromPath("firestore.rules", rulesFile);
            });

            emulatorConfig.firebaseConfig().firestoreConfig.indexesFile.ifPresent(indexesFile -> {
                this.dockerBuilder.add("firestore.indexes.json", FIREBASE_ROOT + "/firestore.indexes.json");
                this.result.withFileFromPath("firestore.indexes.json", indexesFile);
            });
        }

        private void includeStorageFiles() {
            emulatorConfig.firebaseConfig().storageConfig.rulesFile.ifPresent(rulesFile -> {
                this.dockerBuilder.add("storage.rules", FIREBASE_ROOT + "/storage.rules");
                this.result.withFileFromPath("storage.rules", rulesFile);
            });
        }

        private void generateFirebaseJson() {
            var firebaseJsonBuilder = new FirebaseJsonBuilder(this.emulatorConfig);
            String firebaseJson;
            try {
                firebaseJson = firebaseJsonBuilder.buildFirebaseConfig();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to generate firebase.json file", e);
            }

            this.result.withFileFromString("firebase.json", firebaseJson);
        }

        private void setupDataImportExport() {
            emulatorConfig.emulatorData().ifPresent(emulator -> this.dockerBuilder.volume(EMULATOR_DATA_PATH));
        }

        private void setupHosting() {
            // Specify public directory if hosting is enabled
            if (emulatorConfig.firebaseConfig().hostingConfig().hostingContentDir().isPresent()) {
                this.dockerBuilder.volume(FIREBASE_HOSTING_PATH);
            }
        }

        private void setupUserAndGroup() {
            var commands = new ArrayList<String>();

            emulatorConfig.dockerConfig.groupId().ifPresent(group -> commands.add("addgroup -g " + group + " runner"));

            emulatorConfig.dockerConfig.userId().ifPresent(user -> {
                var groupName = emulatorConfig.dockerConfig().groupId().map(i -> "runner").orElse("node");
                commands.add("adduser -u " + user + " -G " + groupName + " -D -h /srv/firebase runner");
            });

            var group = dockerGroup();
            var user = dockerUser();

            commands.add("chown " + user + ":" + group + " -R /srv/*");

            var runCmd = String.join(" && ", commands);

            dockerBuilder
                    .run(runCmd)
                    .user(user + ":" + group);
        }

        private int dockerUser() {
            return emulatorConfig.dockerConfig().userId().orElse(1000);
        }

        private int dockerGroup() {
            return emulatorConfig.dockerConfig().groupId().orElse(1000);
        }

        private void runExecutable() {
            List<String> arguments = new ArrayList<>();

            arguments.add("emulators:start");

            emulatorConfig.projectId()
                    .map(id -> "--project")
                    .ifPresent(arguments::add);

            emulatorConfig.projectId()
                    .ifPresent(arguments::add);

            emulatorConfig
                    .emulatorData()
                    .map(path -> "--import")
                    .ifPresent(arguments::add);

            /*
             * We write the data to a subdirectory of the mount point. The firebase emulator tries to remove and
             * recreate the mount-point directory, which will obviously fail. By using a subdirectory, export succeeds.
             */
            emulatorConfig
                    .emulatorData()
                    .map(path -> EMULATOR_EXPORT_PATH)
                    .ifPresent(arguments::add);

            emulatorConfig
                    .emulatorData()
                    .map(path -> "--export-on-exit")
                    .ifPresent(arguments::add);

            dockerBuilder.entryPoint(new String[] { "/usr/local/bin/firebase" });
            dockerBuilder.cmd(arguments.toArray(new String[0]));
        }

        private boolean isEmulatorEnabled(Emulator emulator) {
            return this.devServices.containsKey(emulator);
        }
    }

    @Override
    public void stop() {
        /*
         * We override the way test containers stops the container. By default, test containers will send a
         * kill (SIGKILL) command instead of a stop (SIGTERM) command. This will kill the container instantly
         * and prevent firebase from writing the "--export-on-exit" data to the mounted directory.
         */
        this.getDockerClient().stopContainerCmd(this.getContainerId()).exec();

        super.stop();
    }

    /**
     * Configures the Pub/Sub emulator container.
     */
    @Override
    public void configure() {
        super.configure();

        services.keySet()
                .forEach(emulator -> {
                    var exposedPort = services.get(emulator);
                    // Expose emulatorPort
                    if (exposedPort.isFixed()) {
                        addFixedExposedPort(exposedPort.fixedPort(), exposedPort.fixedPort());
                    } else {
                        addExposedPort(emulator.internalPort);
                    }
                });

        waitingFor(Wait.forLogMessage(".*Emulator Hub running at.*", 1));
    }

    public Map<Emulator, String> emulatorEndpoints() {
        return services.keySet()
                .stream()
                .collect(Collectors.toMap(
                        e -> e,
                        this::getEmulatorEndpoint));
    }

    public Integer emulatorPort(Emulator emulator) {
        var exposedPort = services.get(emulator);
        if (exposedPort.isFixed()) {
            return exposedPort.fixedPort();
        } else {
            return getMappedPort(emulator.internalPort);
        }
    }

    public Map<Emulator, Integer> emulatorPorts() {
        return services.keySet()
                .stream()
                .collect(Collectors.toMap(
                        e -> e,
                        this::emulatorPort));
    }

    private String getEmulatorEndpoint(Emulator emulator) {
        return this.getHost() + ":" + emulatorPort(emulator);
    }
}
