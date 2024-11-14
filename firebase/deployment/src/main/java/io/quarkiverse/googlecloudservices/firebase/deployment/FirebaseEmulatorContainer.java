package io.quarkiverse.googlecloudservices.firebase.deployment;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

        final int internalPort;
        final String configProperty;
        final String emulatorName;

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
     * Describes the Firebase emulator configuration.
     *
     * @param dockerConfig The docker configuration
     * @param firebaseVersion The firebase version to use
     * @param projectId The project ID, needed when running with the auth emulator
     * @param token The Google Cloud CLI token to use for authentication. Needed for firebase hosting
     * @param customFirebaseJson The path to a custom firebase
     * @param javaToolOptions The options to pass to the java based emulators
     * @param emulatorData The path to the directory where to store the emulator data
     * @param hostingContentDir The path to the directory containing the hosting content
     */
    public record EmulatorConfig(
            DockerConfig dockerConfig,
            String firebaseVersion,
            Optional<String> projectId,
            Optional<String> token,
            Optional<Path> customFirebaseJson,
            Optional<String> javaToolOptions,
            Optional<Path> emulatorData,
            Optional<Path> hostingContentDir,
            Map<Emulator, ExposedPort> services) {
    }

    private final Map<Emulator, ExposedPort> services;

    /**
     * Creates a new Firebase Emulator container
     *
     * @param firebaseConfig The generic configuration of the firebase emulators
     */
    public FirebaseEmulatorContainer(EmulatorConfig firebaseConfig) {
        super(new FirebaseDockerBuilder(firebaseConfig).build());

        firebaseConfig.emulatorData().ifPresent(path -> {
            // https://firebase.google.com/docs/emulator-suite/install_and_configure#export_and_import_emulator_data
            // Mount the volume to the specified path
            this.withFileSystemBind(path.toString(), EMULATOR_DATA_PATH, BindMode.READ_WRITE);
        });

        firebaseConfig.hostingContentDir().ifPresent(hostingPath -> {
            // Mount volume for static hosting content
            this.withFileSystemBind(hostingPath.toString(), FIREBASE_HOSTING_PATH, BindMode.READ_ONLY);
        });

        this.services = firebaseConfig.services;
    }

    private static class FirebaseDockerBuilder {

        private final ImageFromDockerfile result;

        private final EmulatorConfig firebaseConfig;
        private final Map<Emulator, ExposedPort> devServices;

        private DockerfileBuilder dockerBuilder;

        public FirebaseDockerBuilder(EmulatorConfig firebaseConfig) {
            this.devServices = firebaseConfig.services;
            this.firebaseConfig = firebaseConfig;

            this.result = new ImageFromDockerfile("localhost/testcontainers/firebase", false)
                    .withDockerfileFromBuilder(builder -> this.dockerBuilder = builder);
        }

        public ImageFromDockerfile build() {
            this.validateConfiguration();
            this.configureBaseImage();
            this.installNeededSoftware();
            this.clearUnneededUsersAndGroups();
            this.authenticateToFirebase();
            this.setupJavaToolOptions();
            this.addFirebaseJson();
            this.fixFilePermissions();
            this.setupUserAndGroup();
            this.downloadEmulators();
            this.setupDataImportExport();
            this.setupHosting();
            this.runExecutable();

            return result;
        }

        private void validateConfiguration() {
            if (isEmulatorEnabled(Emulator.AUTHENTICATION) && firebaseConfig.projectId().isEmpty()) {
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
            dockerBuilder.from(firebaseConfig.dockerConfig().imageName());
        }

        private void installNeededSoftware() {
            dockerBuilder
                    .run("apk --no-cache add openjdk11-jre bash curl openssl gettext nano nginx sudo && " +
                            "npm cache clean --force && " +
                            "npm i -g firebase-tools@" + firebaseConfig.firebaseVersion());
        }

        private void clearUnneededUsersAndGroups() {
            dockerBuilder.run("deluser nginx && delgroup abuild && delgroup ping");
        }

        private void downloadEmulators() {
            downloadEmulator(Emulator.REALTIME_DATABASE, "database");
            downloadEmulator(Emulator.CLOUD_FIRESTORE, "firestore");
            downloadEmulator(Emulator.PUB_SUB, "pubsub");
            downloadEmulator(Emulator.CLOUD_STORAGE, "storage");
            downloadEmulator(Emulator.EMULATOR_SUITE_UI, "ui");
        }

        private void downloadEmulator(Emulator emulator, String downloadId) {
            if (isEmulatorEnabled(emulator)) {
                dockerBuilder.run("firebase setup:emulators:" + downloadId);
            }
        }

        private void authenticateToFirebase() {
            firebaseConfig.token().ifPresent(token -> dockerBuilder.env("FIREBASE_TOKEN", token));
        }

        private void setupJavaToolOptions() {
            firebaseConfig.javaToolOptions().ifPresent(toolOptions -> dockerBuilder.env("JAVA_TOOL_OPTIONS", toolOptions));
        }

        private void addFirebaseJson() {
            dockerBuilder.workDir(FIREBASE_ROOT);

            firebaseConfig.customFirebaseJson().ifPresentOrElse(
                    this::includeCustomFirebaseJson,
                    this::generateFirebaseJson);

            this.dockerBuilder.add("firebase.json", FIREBASE_ROOT + "/firebase.json");
        }

        private void includeCustomFirebaseJson(Path customFilePath) {
            this.result.withFileFromPath(
                    "firebase.json",
                    customFilePath);
        }

        private void generateFirebaseJson() {
            StringBuilder firebaseJson = new StringBuilder();

            firebaseJson.append("{\n");
            firebaseJson.append("\t\"emulators\": {\n");

            var emulatorsJson = this.devServices
                    .entrySet()
                    .stream()
                    .filter(service -> service.getKey().configProperty != null)
                    .map((service -> {
                        var emulator = service.getKey();

                        var port = Optional.ofNullable(service.getValue().fixedPort())
                                .orElse(emulator.internalPort);

                        String additionalConfig = "";
                        if (emulator.equals(Emulator.CLOUD_FIRESTORE)) {
                            var wsService = this.devServices.get(Emulator.CLOUD_FIRESTORE_WS);
                            if (wsService != null) {
                                var wsPort = Optional.ofNullable(wsService.fixedPort)
                                        .orElse(Emulator.CLOUD_FIRESTORE.internalPort);
                                additionalConfig = "\t\t\t\"websocketPort\": " + wsPort + ",\n";
                            }
                        }

                        return "\t\t\"" + emulator.configProperty + "\": {\n" +
                                "\t\t\t\"port\": " + port + ",\n" +
                                additionalConfig +
                                "\t\t\t\"host\": \"0.0.0.0\"\n" +
                                "\t\t}";
                    }))
                    .collect(Collectors.joining(",\n"));
            firebaseJson.append(emulatorsJson).append("\n");
            firebaseJson.append("\t}\n");

            if (isEmulatorEnabled(Emulator.CLOUD_FIRESTORE)) {
                var firestoreJson = ",\"firestore\": {}\n";
                firebaseJson.append(firestoreJson);
            }

            firebaseJson.append("}\n");

            this.result.withFileFromString("firebase.json", firebaseJson.toString());
        }

        private void setupDataImportExport() {
            firebaseConfig.emulatorData().ifPresent(emulator -> {
                this.dockerBuilder.run("mkdir -p " + EMULATOR_DATA_PATH);
                this.dockerBuilder.volume(EMULATOR_DATA_PATH);
            });
        }

        private void setupHosting() {
            // Specify public directory if hosting is enabled
            if (firebaseConfig.hostingContentDir().isPresent()) {
                dockerBuilder.run("mkdir -p " + FIREBASE_HOSTING_PATH);
            }
        }

        private void fixFilePermissions() {
            var group = dockerGroup();
            var user = dockerUser();

            dockerBuilder.run("chown " + user + ":" + group + " -R /srv/*");
        }

        private void setupUserAndGroup() {
            firebaseConfig.dockerConfig.groupId().ifPresent(group -> {
                dockerBuilder.run("addgroup -g " + group + " runner");
            });

            firebaseConfig.dockerConfig.userId().ifPresent(user -> {
                var groupName = firebaseConfig.dockerConfig().groupId().map(i -> "runner").orElse("node");
                dockerBuilder.run("adduser -u " + user + " -G " + groupName + " -D -h /srv/firebase runner");
            });

            var group = dockerGroup();
            var user = dockerUser();

            dockerBuilder.user(user + ":" + group);
        }

        private int dockerUser() {
            return firebaseConfig.dockerConfig().userId().orElse(1000);
        }

        private int dockerGroup() {
            return firebaseConfig.dockerConfig().groupId().orElse(1000);
        }

        private void runExecutable() {
            List<String> arguments = new ArrayList<>();

            arguments.add("emulators:start");

            firebaseConfig.projectId()
                    .map(id -> "--project")
                    .ifPresent(arguments::add);

            firebaseConfig.projectId()
                    .ifPresent(arguments::add);

            firebaseConfig
                    .emulatorData()
                    .map(path -> "--import")
                    .ifPresent(arguments::add);

            /*
             * We write the data to a subdirectory of the mount point. The firebase emulator tries to remove and
             * recreate the mount-point directory, which will obviously fail. By using a subdirectory, export succeeds.
             */
            firebaseConfig
                    .emulatorData()
                    .map(path -> EMULATOR_DATA_PATH + "/emulator-data")
                    .ifPresent(arguments::add);

            firebaseConfig
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
