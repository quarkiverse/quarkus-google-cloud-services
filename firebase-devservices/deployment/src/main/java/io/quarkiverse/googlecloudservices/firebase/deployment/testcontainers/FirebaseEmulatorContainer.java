package io.quarkiverse.googlecloudservices.firebase.deployment.testcontainers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.images.builder.dockerfile.DockerfileBuilder;

/**
 * Testcontainers container to run Firebase emulators from Docker.
 */
public class FirebaseEmulatorContainer extends GenericContainer<FirebaseEmulatorContainer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FirebaseEmulatorContainer.class);

    private static final String FIREBASE_ROOT = "/srv/firebase";
    private static final String FIREBASE_HOSTING_PATH = FIREBASE_ROOT + "/" + FirebaseJsonBuilder.FIREBASE_HOSTING_SUBPATH;
    private static final String EMULATOR_DATA_PATH = FIREBASE_ROOT + "/data";
    private static final String EMULATOR_EXPORT_PATH = EMULATOR_DATA_PATH + "/emulator-data";

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

        /**
         * The default port on which the emulator is running.
         */
        public final int internalPort;
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

        public static final ExposedPort RANDOM_PORT = new ExposedPort(null);

        boolean isFixed() {
            return fixedPort != null;
        }
    }

    /**
     * The docker image configuration
     *
     * @param imageName The name of the docker image
     * @param userId The user id to run the docker image
     * @param groupId The group id to run the docker image
     * @param followStdOut Pipe stdout of the container to stdout of the host
     * @param followStdErr Pipe stderr of the container to stderr of the host
     * @param afterStart Callback to handle additional logic after the container has started.
     */
    public record DockerConfig(
            String imageName,
            Optional<Integer> userId,
            Optional<Integer> groupId,
            boolean followStdOut,
            boolean followStdErr,
            Consumer<FirebaseEmulatorContainer> afterStart) {

        /**
         * Default settings
         */
        public static final DockerConfig DEFAULT = new DockerConfig(
                DEFAULT_IMAGE_NAME,
                Optional.empty(),
                Optional.empty(),
                true,
                true,
                null);
    }

    /**
     * Record to hold the argument for the CLI executable.
     *
     * @param projectId The project ID, needed when running with the auth emulator
     * @param token The Google Cloud CLI token to use for authentication. Needed for firebase hosting
     * @param javaToolOptions The options to pass to the java based emulators
     * @param emulatorData The path to the directory where to store the emulator data
     * @param importExport Specify whether to import, export or do both with the emulator data
     * @param debug Whether to run with the --debug flag
     */
    public record CliArgumentsConfig(
            Optional<String> projectId,
            Optional<String> token,
            Optional<String> javaToolOptions,
            Optional<Path> emulatorData,
            ImportExport importExport,
            boolean debug) {
        public static final CliArgumentsConfig DEFAULT = new CliArgumentsConfig(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                ImportExport.IMPORT_EXPORT,
                false);
    }

    /**
     * Behaviour of the import/export data.
     */
    public enum ImportExport {
        /**
         * Only import the data
         */
        IMPORT_ONLY(true, false),

        /**
         * Only export the data
         */
        EXPORT_ONLY(false, true),

        /**
         * Both import and export the data.
         */
        IMPORT_EXPORT(true, true);

        private final boolean doImport;
        private final boolean doExport;

        ImportExport(boolean doImport, boolean doExport) {
            this.doImport = doImport;
            this.doExport = doExport;
        }

        boolean isDoImport() {
            return doImport;
        }

        boolean isDoExport() {
            return doExport;
        }
    }

    /**
     * Firebase hosting configuration
     *
     * @param hostingContentDir The path to the directory containing the hosting content
     */
    public record HostingConfig(
            Optional<Path> hostingContentDir) {

        public static final HostingConfig DEFAULT = new HostingConfig(
                Optional.empty());
    }

    /**
     * Cloud storage configuration
     *
     * @param rulesFile Cloud storage rules file
     */
    public record StorageConfig(
            Optional<Path> rulesFile) {

        public static final StorageConfig DEFAULT = new StorageConfig(
                Optional.empty());
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

        public static final FirestoreConfig DEFAULT = new FirestoreConfig(
                Optional.empty(),
                Optional.empty());
    }

    /**
     * Functions configuration
     *
     * @param functionsPath The location for the functions sources
     * @param ignores The files to ignore when creating the function
     */
    public record FunctionsConfig(
            Optional<Path> functionsPath,
            String[] ignores) {

        public static FunctionsConfig DEFAULT = new FunctionsConfig(
                Optional.empty(),
                new String[0]);
    }

    /**
     * The firebase configuration, this record mimics the various items which can be configured using the
     * firebase.json file.
     *
     * @param hostingConfig The firebase hosting configuration
     * @param storageConfig The storage configuration
     * @param firestoreConfig The firestore configuration
     * @param functionsConfig The functions configuration
     * @param services The exposed services configuration
     */
    public record FirebaseConfig(
            HostingConfig hostingConfig,
            StorageConfig storageConfig,
            FirestoreConfig firestoreConfig,
            FunctionsConfig functionsConfig,
            Map<Emulator, ExposedPort> services) {
    }

    /**
     * Describes the Firebase emulator configuration.
     *
     * @param dockerConfig The docker configuration
     * @param firebaseVersion The firebase version to use
     * @param cliArguments The arguments to the CLI
     * @param customFirebaseJson The path to a custom firebase
     * @param firebaseConfig The firebase configuration
     */
    public record EmulatorConfig(
            DockerConfig dockerConfig,
            String firebaseVersion,
            CliArgumentsConfig cliArguments,
            Optional<Path> customFirebaseJson,
            FirebaseConfig firebaseConfig) {
    }

    // Use node:20 for now because of https://github.com/firebase/firebase-tools/issues/7173
    /**
     * The default image to use for building the docker image.
     */
    public static final String DEFAULT_IMAGE_NAME = "node:20-alpine";
    /**
     * The default version of the firebase tools to install.
     */
    public static final String DEFAULT_FIREBASE_VERSION = "latest";

    /**
     * Builder for the {@link FirebaseEmulatorContainer} configuration.
     */
    public static class Builder {

        private DockerConfig dockerConfig = DockerConfig.DEFAULT;
        private String firebaseVersion = DEFAULT_FIREBASE_VERSION;
        private CliArgumentsConfig cliArguments = CliArgumentsConfig.DEFAULT;

        private Path customFirebaseJson;
        private FirebaseConfig firebaseConfig;

        private Builder() {
        }

        /**
         * Configure the docker options
         *
         * @return THe docker config builder
         */
        public DockerConfigBuilder withDockerConfig() {
            return new DockerConfigBuilder();
        }

        /**
         * Configure the firebase version
         *
         * @param firebaseVersion The firebase version
         * @return The builder
         */
        public Builder withFirebaseVersion(String firebaseVersion) {
            this.firebaseVersion = firebaseVersion;
            return this;
        }

        /**
         * Configure the CLI argument options
         *
         * @return The CLI Builder
         */
        public CliBuilder withCliArguments() {
            return new CliBuilder();
        }

        /**
         * Read the configuration from the custom firebase.json file.
         *
         * @param customFirebaseJson The path to the custom firebase json
         * @return The builder
         * @throws IOException In case the file could not be read.
         */
        public Builder readFromFirebaseJson(Path customFirebaseJson) throws IOException {
            var reader = new CustomFirebaseConfigReader();
            this.firebaseConfig = reader.readFromFirebase(customFirebaseJson);
            this.customFirebaseJson = customFirebaseJson;
            return this;
        }

        /**
         * Configure the firebase emulators
         *
         * @return The firebase config builder
         */
        public FirebaseConfigBuilder withFirebaseConfig() {
            return new FirebaseConfigBuilder();
        }

        /**
         * Build the configuration
         *
         * @return The emulator configuration.
         */
        public EmulatorConfig buildConfig() {
            if (firebaseConfig == null) {
                // Try to autoload the firebase.json configuration
                var defaultFirebaseJson = new File("firebase.json").getAbsoluteFile().toPath();

                LOGGER.info("Trying to automatically read firebase config from {}", defaultFirebaseJson);

                try {
                    readFromFirebaseJson(defaultFirebaseJson);
                } catch (IOException e) {
                    throw new IllegalStateException(
                            "Firebase was not configured and could not auto-read from " + defaultFirebaseJson);
                }
            }

            return new EmulatorConfig(
                    dockerConfig,
                    firebaseVersion,
                    cliArguments,
                    Optional.ofNullable(customFirebaseJson),
                    firebaseConfig);
        }

        /**
         * Build the final configuration
         *
         * @return the final configuration.
         */
        public FirebaseEmulatorContainer build() {
            return new FirebaseEmulatorContainer(buildConfig());
        }

        /**
         * Builder for the docker configuration.
         */
        public class DockerConfigBuilder {

            private DockerConfigBuilder() {
            }

            /**
             * Configure the base image to use
             *
             * @param imageName The image name
             * @return The builder
             */
            public DockerConfigBuilder withImage(String imageName) {
                Builder.this.dockerConfig = new DockerConfig(
                        imageName,
                        Builder.this.dockerConfig.userId(),
                        Builder.this.dockerConfig.groupId(),
                        Builder.this.dockerConfig.followStdOut(),
                        Builder.this.dockerConfig.followStdErr(),
                        Builder.this.dockerConfig.afterStart());
                return this;
            }

            /**
             * Configure the user id to use within docker
             *
             * @param userId The user id
             * @return The builder
             */
            public DockerConfigBuilder withUserId(int userId) {
                return withUserId(Optional.of(userId));
            }

            /**
             * Try to configure the user id to use within docker from an environment variable.
             *
             * @param env The environment variable
             * @return The builder
             */
            public DockerConfigBuilder withUserIdFromEnv(String env) {
                return withUserId(readIdFromEnv(env));
            }

            private DockerConfigBuilder withUserId(Optional<Integer> userId) {
                Builder.this.dockerConfig = new DockerConfig(
                        Builder.this.dockerConfig.imageName(),
                        userId,
                        Builder.this.dockerConfig.groupId(),
                        Builder.this.dockerConfig.followStdOut(),
                        Builder.this.dockerConfig.followStdErr(),
                        Builder.this.dockerConfig.afterStart());
                return this;
            }

            /**
             * Configure the group id to use within docker
             *
             * @param groupId The group id
             * @return The builder
             */
            public DockerConfigBuilder withGroupId(int groupId) {
                return withGroupId(Optional.of(groupId));
            }

            /**
             * Try to configure the group id to use within docker from an environment variable.
             *
             * @param env The environment variable
             * @return The builder
             */
            public DockerConfigBuilder withGroupIdFromEnv(String env) {
                return withGroupId(readIdFromEnv(env));
            }

            private DockerConfigBuilder withGroupId(Optional<Integer> groupId) {
                Builder.this.dockerConfig = new DockerConfig(
                        Builder.this.dockerConfig.imageName(),
                        Builder.this.dockerConfig.userId(),
                        groupId,
                        Builder.this.dockerConfig.followStdOut(),
                        Builder.this.dockerConfig.followStdErr(),
                        Builder.this.dockerConfig.afterStart());
                return this;
            }

            /**
             * Pipe the container stdout to the host stdout. This can ease debugging of container issues.
             *
             * @param followStdOut Whether to pipe the container stdout to the host stdout
             * @return The builder
             */
            public DockerConfigBuilder followStdOut(boolean followStdOut) {
                Builder.this.dockerConfig = new DockerConfig(
                        Builder.this.dockerConfig.imageName(),
                        Builder.this.dockerConfig.userId(),
                        Builder.this.dockerConfig.groupId(),
                        followStdOut,
                        Builder.this.dockerConfig.followStdErr(),
                        Builder.this.dockerConfig.afterStart());
                return this;
            }

            /**
             * Pipe the container stdout to the host stderr. This can ease debugging of container issues.
             *
             * @param followStdErr Whether to pipe the container stderr to the host stdout
             * @return The builder
             */
            public DockerConfigBuilder followStdErr(boolean followStdErr) {
                Builder.this.dockerConfig = new DockerConfig(
                        Builder.this.dockerConfig.imageName(),
                        Builder.this.dockerConfig.userId(),
                        Builder.this.dockerConfig.groupId(),
                        Builder.this.dockerConfig.followStdOut(),
                        followStdErr,
                        Builder.this.dockerConfig.afterStart());
                return this;
            }

            /**
             * Set a callback to run after the container has started.
             *
             * @param afterStart Callback to be executed after the container has started
             * @return The builder
             */
            public DockerConfigBuilder afterStart(Consumer<FirebaseEmulatorContainer> afterStart) {
                Builder.this.dockerConfig = new DockerConfig(
                        Builder.this.dockerConfig.imageName(),
                        Builder.this.dockerConfig.userId(),
                        Builder.this.dockerConfig.groupId(),
                        Builder.this.dockerConfig.followStdOut(),
                        Builder.this.dockerConfig.followStdErr(),
                        afterStart);
                return this;
            }

            /**
             * Finish the docker configuration
             *
             * @return The primary builder
             */
            public Builder done() {
                return Builder.this;
            }

            private Optional<Integer> readIdFromEnv(String env) {
                try {
                    return Optional
                            .ofNullable(System.getenv(env))
                            .map(Integer::valueOf);
                } catch (NumberFormatException e) {
                    return Optional.empty();
                }
            }
        }

        /**
         * Builder for the CLI Arguments configuration
         */
        public class CliBuilder {
            private String projectId;
            private String token;
            private String javaToolOptions;
            private Path emulatorData;
            private ImportExport importExport;
            private boolean debug;

            /**
             * The CLI Builder constructor
             */
            private CliBuilder() {
                this.projectId = Builder.this.cliArguments.projectId.orElse(null);
                this.token = Builder.this.cliArguments.token.orElse(null);
                this.javaToolOptions = Builder.this.cliArguments.javaToolOptions.orElse(null);
                this.emulatorData = Builder.this.cliArguments.emulatorData.orElse(null);
                this.importExport = Builder.this.cliArguments.importExport;
                this.debug = Builder.this.cliArguments.debug;
            }

            /**
             * Configure the project id
             *
             * @param projectId The project id
             * @return The builder
             */
            public CliBuilder withProjectId(String projectId) {
                this.projectId = projectId;
                return this;
            }

            /**
             * Configure the Google auth token to use
             *
             * @param token The token
             * @return The builder
             */
            public CliBuilder withToken(String token) {
                this.token = token;
                return this;
            }

            /**
             * Configure the java tool options
             *
             * @param javaToolOptions The java tool options
             * @return The builder
             */
            public CliBuilder withJavaToolOptions(String javaToolOptions) {
                this.javaToolOptions = javaToolOptions;
                return this;
            }

            /**
             * Configure the location to import/export the emulator data
             *
             * @param emulatorData The emulator data
             * @return The builder
             */
            public CliBuilder withEmulatorData(Path emulatorData) {
                this.emulatorData = emulatorData;
                return this;
            }

            /**
             * Set the import/export behaviour for the specified emulator data. This setting is inactive unless
             * {@link #withEmulatorData(Path)} is set.
             *
             * @param importExport The import/export setting
             * @return The builder
             */
            public CliBuilder withImportExport(ImportExport importExport) {
                this.importExport = importExport;
                return this;
            }

            /**
             * Run the firebase tools with a debug flag
             *
             * @param debug Whether to run with debug or not
             * @return The builder
             */
            public CliBuilder withDebug(boolean debug) {
                this.debug = debug;
                return this;
            }

            /**
             * Finish the builder
             *
             * @return The parent builder
             */
            public Builder done() {
                Builder.this.cliArguments = new CliArgumentsConfig(
                        Optional.ofNullable(this.projectId),
                        Optional.ofNullable(this.token),
                        Optional.ofNullable(this.javaToolOptions),
                        Optional.ofNullable(this.emulatorData),
                        this.importExport,
                        this.debug);
                return Builder.this;
            }
        }

        /**
         * Builder for the Firebase configuration
         */
        public class FirebaseConfigBuilder {

            private HostingConfig hostingConfig = HostingConfig.DEFAULT;
            private StorageConfig storageConfig = StorageConfig.DEFAULT;
            private FirestoreConfig firestoreConfig = FirestoreConfig.DEFAULT;
            private FunctionsConfig functionsConfig = FunctionsConfig.DEFAULT;
            private final Map<Emulator, ExposedPort> services = new HashMap<>();

            /**
             * Create a new builder
             */
            public FirebaseConfigBuilder() {
            }

            /**
             * Configure the directory where to find the hosting files
             *
             * @param hostingContentDir The hosting directory
             * @return The builder
             */
            public FirebaseConfigBuilder withHostingPath(Path hostingContentDir) {
                this.hostingConfig = new HostingConfig(
                        Optional.of(hostingContentDir));
                return this;
            }

            /**
             * Configure the Google Cloud storage rules file
             *
             * @param rulesFile The rules file.
             * @return The builder
             */
            public FirebaseConfigBuilder withStorageRules(Path rulesFile) {
                this.storageConfig = new StorageConfig(
                        Optional.of(rulesFile));
                return this;
            }

            /**
             * Configure the Firestore rules file
             *
             * @param rulesFile The rules file
             * @return The builder
             */
            public FirebaseConfigBuilder withFirestoreRules(Path rulesFile) {
                this.firestoreConfig = new FirestoreConfig(
                        Optional.of(rulesFile),
                        this.firestoreConfig.indexesFile);
                return this;
            }

            /**
             * Configure the firestore indexes file
             *
             * @param indexes The indexes file
             * @return The builder
             */
            public FirebaseConfigBuilder withFirestoreIndexes(Path indexes) {
                this.firestoreConfig = new FirestoreConfig(
                        this.firestoreConfig.rulesFile(),
                        Optional.of(indexes));
                return this;
            }

            /**
             * Configure the input directory for the functions
             *
             * @param functions The path to the functions
             * @return The builder
             */
            public FirebaseConfigBuilder withFunctionsFromPath(Path functions) {
                this.functionsConfig = new FunctionsConfig(
                        Optional.of(functions),
                        this.functionsConfig.ignores());
                return this;
            }

            /**
             * Configure the ignores for the functions directory
             *
             * @param ignores The ignores
             * @return The builder
             */
            public FirebaseConfigBuilder withFunctionIgnores(String[] ignores) {
                this.functionsConfig = new FunctionsConfig(
                        this.functionsConfig.functionsPath,
                        ignores);
                return this;
            }

            /**
             * Include an emulator on a random port
             *
             * @param emulator The emulator
             * @return The builder
             */
            public FirebaseConfigBuilder withEmulator(Emulator emulator) {
                this.services.put(emulator, ExposedPort.RANDOM_PORT);
                return this;
            }

            /**
             * Include emulators on a random port
             *
             * @param emulators The emulators
             * @return The builder
             */
            public FirebaseConfigBuilder withEmulators(Emulator... emulators) {
                for (Emulator emulator : emulators) {
                    withEmulator(emulator);
                }
                return this;
            }

            /**
             * Include an emulator on a fixed port
             *
             * @param emulator The emulator
             * @param port The port to expose on
             * @return The builder
             */
            public FirebaseConfigBuilder withEmulatorOnFixedPort(Emulator emulator, int port) {
                this.services.put(emulator, new ExposedPort(port));
                return this;
            }

            /**
             * Include emulators on fixed ports
             *
             * @param emulatorsAndPorts Alternating the {@link Emulator} and the {@link Integer} port.
             * @return The builder
             * @throws IllegalArgumentException In case the arguments don't alternate between Emulator and Port.
             */
            public FirebaseConfigBuilder withEmulatorsOnPorts(Object... emulatorsAndPorts) {
                if (emulatorsAndPorts.length % 2 != 0) {
                    throw new IllegalArgumentException("Emulators and ports must both be specified alternating");
                }

                try {
                    for (int i = 0; i < emulatorsAndPorts.length; i += 2) {
                        var emulator = (Emulator) emulatorsAndPorts[i];
                        var port = (Integer) emulatorsAndPorts[i + 1];
                        withEmulatorOnFixedPort(emulator, port);
                    }
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException("Emulators and ports must be specified alternating");
                }

                return this;
            }

            /**
             * Finish the firebase configuration
             *
             * @return The primary builder
             */
            public Builder done() {
                Builder.this.firebaseConfig = new FirebaseConfig(
                        hostingConfig,
                        storageConfig,
                        firestoreConfig,
                        functionsConfig,
                        services);
                Builder.this.customFirebaseJson = null;

                return Builder.this;
            }
        }
    }

    private final Map<Emulator, ExposedPort> services;
    private final boolean followStdOut;
    private final boolean followStdErr;
    private final Consumer<FirebaseEmulatorContainer> afterStart;

    /**
     * Create the builder for the emulator container
     *
     * @return The builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new Firebase Emulator container
     *
     * @param emulatorConfig The generic configuration of the firebase emulators
     */
    public FirebaseEmulatorContainer(EmulatorConfig emulatorConfig) {
        super(new FirebaseDockerBuilder(emulatorConfig).build());

        this.services = emulatorConfig.firebaseConfig().services;
        this.followStdOut = emulatorConfig.dockerConfig().followStdOut();
        this.followStdErr = emulatorConfig.dockerConfig().followStdErr();
        this.afterStart = emulatorConfig.dockerConfig().afterStart();

        emulatorConfig.cliArguments().emulatorData().ifPresent(path -> {
            // https://firebase.google.com/docs/emulator-suite/install_and_configure#export_and_import_emulator_data
            // Mount the volume to the specified path
            this.withFileSystemBind(path.toString(), EMULATOR_DATA_PATH, BindMode.READ_WRITE);
        });

        if (this.services.containsKey(Emulator.FIREBASE_HOSTING)) {
            var hostingPath = emulatorConfig
                    .firebaseConfig()
                    .hostingConfig()
                    .hostingContentDir()
                    .map(Path::toString)
                    .orElse(new File(FirebaseJsonBuilder.FIREBASE_HOSTING_SUBPATH).getAbsolutePath());

            LOGGER.debug("Mounting {} to the container hosting path", hostingPath);

            // Mount volume for static hosting content
            this.withFileSystemBind(hostingPath, containerHostingPath(emulatorConfig), BindMode.READ_ONLY);
        }

        if (this.services.containsKey(Emulator.CLOUD_FUNCTIONS)) {
            var functionsPath = emulatorConfig
                    .firebaseConfig()
                    .functionsConfig()
                    .functionsPath()
                    .map(Path::toString)
                    .orElse(new File(FirebaseJsonBuilder.FIREBASE_FUNCTIONS_SUBPATH).getAbsolutePath());

            LOGGER.debug("Mounting {} to the container functions sources path", functionsPath);

            // Mount volume for functions
            this.withFileSystemBind(functionsPath, containerFunctionsPath(emulatorConfig), BindMode.READ_ONLY);
        }
    }

    static String containerHostingPath(EmulatorConfig emulatorConfig) {
        var hostingPath = emulatorConfig.firebaseConfig().hostingConfig().hostingContentDir();
        if (emulatorConfig.customFirebaseJson().isPresent()) {
            var firebaseJsonDir = emulatorConfig.customFirebaseJson().get().getParent();
            hostingPath = hostingPath.map(path -> path.subpath(firebaseJsonDir.getNameCount(), path.getNameCount()));
        }

        if (hostingPath.isPresent()) {
            var path = hostingPath.get();
            if (path.isAbsolute()) {
                return FIREBASE_HOSTING_PATH;
            } else {
                return FIREBASE_ROOT + "/" + hostingPath.get();
            }
        } else {
            return FIREBASE_HOSTING_PATH;
        }
    }

    static String containerFunctionsPath(EmulatorConfig emulatorConfig) {
        var functionsPath = emulatorConfig.firebaseConfig().functionsConfig().functionsPath();
        if (emulatorConfig.customFirebaseJson().isPresent()) {
            var firebaseJsonDir = emulatorConfig.customFirebaseJson().get().getParent();
            functionsPath = functionsPath.map(path -> path.subpath(firebaseJsonDir.getNameCount(), path.getNameCount()));
        }
        return FIREBASE_ROOT + "/" + functionsPath
                .map(Path::toString)
                .orElse(FirebaseJsonBuilder.FIREBASE_FUNCTIONS_SUBPATH);
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
            this.setupFunctions();
            this.runExecutable();

            return result;
        }

        private void validateConfiguration() {
            if (isEmulatorEnabled(Emulator.AUTHENTICATION) && emulatorConfig.cliArguments().projectId().isEmpty()) {
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

            if (emulatorConfig.customFirebaseJson.isPresent()) {
                var hostingDir = emulatorConfig.firebaseConfig.hostingConfig.hostingContentDir;

                var hostingDirIsAbsolute = hostingDir
                        .map(Path::isAbsolute)
                        .orElse(false);

                LOGGER.debug("Checking if path {} is absolute --> {}", hostingDir, hostingDirIsAbsolute);

                if (hostingDirIsAbsolute) {
                    throw new IllegalStateException(
                            "When using a custom firebase.json, the hosting path must be relative to the firebase.json file");
                }

                var firebasePath = emulatorConfig.customFirebaseJson.get().toAbsolutePath().getParent();

                var hostingDirIsChildOfFirebaseJsonParent = hostingDir
                        .map(Path::toAbsolutePath)
                        .map(h -> h.startsWith(firebasePath))
                        .orElse(true);

                LOGGER.debug("Checking if the hosting path {} is relative to the firebase.json file --> {}", hostingDir,
                        hostingDirIsChildOfFirebaseJsonParent);

                if (!hostingDirIsChildOfFirebaseJsonParent) {
                    throw new IllegalStateException(
                            "When using a custom firebase.json, the hosting path must be in the same subtree as the firebase.json file");
                }
            }

            if (emulatorConfig.firebaseConfig.functionsConfig.functionsPath.isPresent()) {
                var functionsDir = emulatorConfig.firebaseConfig.functionsConfig.functionsPath;
                var functionsDirIsAbsolute = functionsDir
                        .map(Path::isAbsolute)
                        .orElse(false);

                LOGGER.debug("Checking if the functions sources dir {} is absolute --> {}", functionsDir,
                        functionsDirIsAbsolute);

                if (functionsDirIsAbsolute) {
                    throw new IllegalStateException("Functions path cannot be absolute");
                }
            }

            // TODO: Validate if a custom firebase.json is defined, that the hosts are defined as 0.0.0.0
        }

        private void configureBaseImage() {
            dockerBuilder.from(emulatorConfig.dockerConfig().imageName());
        }

        private void initialSetup() {
            dockerBuilder
                    .run("apk --no-cache add openjdk17-jre bash curl openssl gettext nano nginx sudo && " +
                            "npm cache clean --force && " +
                            "npm i -g firebase-tools@" + emulatorConfig.firebaseVersion() + " && " +
                            "deluser nginx && delgroup abuild && delgroup ping && " +
                            "mkdir -p " + FIREBASE_ROOT + " && " +

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
            emulatorConfig.cliArguments().token().ifPresent(
                    token -> dockerBuilder.env("FIREBASE_TOKEN", token));
        }

        private void setupJavaToolOptions() {
            emulatorConfig.cliArguments().javaToolOptions().ifPresent(
                    toolOptions -> dockerBuilder.env("JAVA_TOOL_OPTIONS", toolOptions));
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
            emulatorConfig.cliArguments().emulatorData().ifPresent(
                    emulator -> this.dockerBuilder.volume(EMULATOR_DATA_PATH));
        }

        private void setupHosting() {
            // Specify public directory if hosting is enabled
            if (emulatorConfig.firebaseConfig().hostingConfig().hostingContentDir().isPresent()) {
                this.dockerBuilder.run("mkdir -p " + containerHostingPath(emulatorConfig));
                this.dockerBuilder.volume(containerHostingPath(emulatorConfig));
            }
        }

        private void setupFunctions() {
            if (emulatorConfig.firebaseConfig().functionsConfig().functionsPath.isPresent()) {
                this.dockerBuilder.run("mkdir -p " + containerFunctionsPath(emulatorConfig));
                this.dockerBuilder.volume(containerFunctionsPath(emulatorConfig));
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

            LOGGER.info("Running docker container as user/group: {}:{}", user, group);

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

            emulatorConfig.cliArguments().projectId()
                    .map(id -> "--project")
                    .ifPresent(arguments::add);

            emulatorConfig.cliArguments().projectId()
                    .ifPresent(arguments::add);

            if (emulatorConfig.cliArguments().debug) {
                arguments.add("--debug");
            }

            if (emulatorConfig.cliArguments().importExport.isDoExport()) {
                emulatorConfig
                        .cliArguments()
                        .emulatorData()
                        .map(path -> "--import")
                        .ifPresent(arguments::add);

                /*
                 * We write the data to a subdirectory of the mount point. The firebase emulator tries to remove and
                 * recreate the mount-point directory, which will obviously fail. By using a subdirectory, export succeeds.
                 */
                emulatorConfig
                        .cliArguments()
                        .emulatorData()
                        .map(path -> EMULATOR_EXPORT_PATH)
                        .ifPresent(arguments::add);
            }

            if (emulatorConfig.cliArguments().importExport.isDoExport()) {
                emulatorConfig
                        .cliArguments()
                        .emulatorData()
                        .map(path -> "--export-on-exit")
                        .ifPresent(arguments::add);

                /*
                 * We write the data to a subdirectory of the mount point. The firebase emulator tries to remove and
                 * recreate the mount-point directory, which will obviously fail. By using a subdirectory, export succeeds.
                 */
                emulatorConfig
                        .cliArguments()
                        .emulatorData()
                        .map(path -> EMULATOR_EXPORT_PATH)
                        .ifPresent(arguments::add);
            }

            dockerBuilder.entryPoint(new String[] { "/usr/local/bin/firebase" });
            dockerBuilder.cmd(arguments.toArray(new String[0]));
        }

        private boolean isEmulatorEnabled(Emulator emulator) {
            return this.devServices.containsKey(emulator);
        }
    }

    /**
     * Override start to handle logging redirection
     */
    @Override
    public void start() {
        super.start();

        if (followStdOut) {
            followOutput(this::writeToStdOut, OutputFrame.OutputType.STDOUT);
        }

        if (followStdErr) {
            followOutput(this::writeToStdErr, OutputFrame.OutputType.STDERR);
        }

        if (afterStart != null) {
            afterStart.accept(this);
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

    /**
     * Get the various endpoints for the emulators. The map values are in the form of a string "host:port".
     *
     * @return The emulator endpoints
     */
    public Map<Emulator, String> emulatorEndpoints() {
        return services.keySet()
                .stream()
                .collect(Collectors.toMap(
                        e -> e,
                        this::getEmulatorEndpoint));
    }

    /**
     * Return the TCP port an emulator is listening on.
     *
     * @param emulator The emulator
     * @return The TC Port
     */
    public Integer emulatorPort(Emulator emulator) {
        var exposedPort = services.get(emulator);
        if (exposedPort.isFixed()) {
            return exposedPort.fixedPort();
        } else {
            return getMappedPort(emulator.internalPort);
        }
    }

    /**
     * Get the ports on which the emulators are running.
     *
     * @return A map {@link Emulator} -> {@link Integer} indicating the TCP port the emulator is running on.
     */
    public Map<Emulator, Integer> emulatorPorts() {
        return services.keySet()
                .stream()
                .collect(Collectors.toMap(
                        e -> e,
                        this::emulatorPort));
    }

    private void writeToStdOut(OutputFrame frame) {
        writeOutputFrame(frame, Level.INFO);
    }

    private void writeToStdErr(OutputFrame frame) {
        writeOutputFrame(frame, Level.ERROR);
    }

    private void writeOutputFrame(OutputFrame frame, Level level) {
        LOGGER.atLevel(level).log(frame.getUtf8StringWithoutLineEnding());
    }

    private String getEmulatorEndpoint(Emulator emulator) {
        return this.getHost() + ":" + emulatorPort(emulator);
    }
}
