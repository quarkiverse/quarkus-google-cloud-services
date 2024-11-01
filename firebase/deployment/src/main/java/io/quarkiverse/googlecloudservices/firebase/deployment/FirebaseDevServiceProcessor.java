package io.quarkiverse.googlecloudservices.firebase.deployment;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.images.builder.dockerfile.DockerfileBuilder;

import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.*;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;

/**
 * Processor responsible for managing Firebase Dev Services.
 * <p>
 * The processor starts the Firebase service in case it's not running.
 */
@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = GlobalDevServicesConfig.Enabled.class)
public class FirebaseDevServiceProcessor {

    private static final Logger LOGGER = Logger.getLogger(FirebaseDevServiceProcessor.class.getName());

    // Running dev service instance
    private static volatile DevServicesResultBuildItem.RunningDevService devService;
    // Configuration for the Firebase Dev service
    private static volatile FirebaseDevServiceConfig config;

    @BuildStep
    public DevServicesResultBuildItem start(DockerStatusBuildItem dockerStatusBuildItem,
            FirebaseDevServiceConfig firebaseBuildTimeConfig,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LaunchModeBuildItem launchMode,
            LoggingSetupBuildItem loggingSetupBuildItem,
            GlobalDevServicesConfig globalDevServicesConfig) {
        // If dev service is running and config has changed, stop the service
        if (devService != null && !firebaseBuildTimeConfig.equals(config)) {
            stopContainer();
        } else if (devService != null) {
            return devService.toBuildItem();
        }

        // Set up log compressor for startup logs
        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Google Cloud Firebase Dev Services Starting:",
                consoleInstalledBuildItem,
                loggingSetupBuildItem);

        // Try starting the container if conditions are met
        try {
            devService = startContainerIfAvailable(dockerStatusBuildItem, firebaseBuildTimeConfig,
                    globalDevServicesConfig.timeout);
        } catch (Throwable t) {
            LOGGER.warn("Unable to start Firebase dev service", t);
            // Dump captured logs in case of an error
            compressor.closeAndDumpCaptured();
            return null;
        } finally {
            compressor.close();
        }

        return devService == null ? null : devService.toBuildItem();
    }

    /**
     * Start the container if conditions are met.
     *
     * @param dockerStatusBuildItem, Docker status
     * @param config, Configuration for the Firebase service
     * @param timeout, Optional timeout for starting the service
     * @return Running service item, or null if the service couldn't be started
     */
    private DevServicesResultBuildItem.RunningDevService startContainerIfAvailable(DockerStatusBuildItem dockerStatusBuildItem,
            FirebaseDevServiceConfig config,
            Optional<Duration> timeout) {

        if (!config.firebase().devservice().preferFirebaseDevServices()) {
            // Firebase service explicitly disabled
            LOGGER.info("Not starting Dev Services for Firebase as it has been disabled in the config.");
            return null;
        }

        if (!isEnabled(config)) {
            // Firebase service implicitly disabled, no emulators enabled.
            LOGGER.info("Not starting Dev Services for Firebase as no emulators are enabled.");
            return null;
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            LOGGER.info("Not starting devservice because docker is not available");
            return null;
        }

        return startContainer(dockerStatusBuildItem, config, timeout);
    }

    private boolean isEnabled(FirebaseDevServiceConfig config) {
        return devServices(config)
                .values()
                .stream()
                .map(FirebaseDevServiceConfig.GenericDevService::enabled)
                .reduce(Boolean.FALSE, Boolean::logicalOr);
    }

    private Map<FirebaseEmulatorContainer.Emulators, FirebaseDevServiceConfig.GenericDevService> devServices(
            FirebaseDevServiceConfig config) {
        return Map.of(
                FirebaseEmulatorContainer.Emulators.AUTHENTICATION, config.firebase().auth().devservice(),
                FirebaseEmulatorContainer.Emulators.EMULATOR_SUITE_UI, config.firebase().devservice().ui(),
                FirebaseEmulatorContainer.Emulators.REALTIME_DATABASE, config.database().devservice(),
                FirebaseEmulatorContainer.Emulators.CLOUD_FIRESTORE, config.firestore().devservice(),
                FirebaseEmulatorContainer.Emulators.CLOUD_FUNCTIONS, config.functions().devservice(),
                FirebaseEmulatorContainer.Emulators.FIREBASE_HOSTING, config.firebase().hosting().devservice(),
                FirebaseEmulatorContainer.Emulators.PUB_SUB, config.pubSub().devservice());
    }

    /**
     * Starts the Pub/Sub emulator container with provided configuration.
     *
     * @param dockerStatusBuildItem, Docker status
     * @param config, Configuration for the PubSub service
     * @param timeout, Optional timeout for starting the service
     * @return Running service item, or null if the service couldn't be started
     */
    private DevServicesResultBuildItem.RunningDevService startContainer(DockerStatusBuildItem dockerStatusBuildItem,
            FirebaseDevServiceConfig config,
            Optional<Duration> timeout) {

        var devServices = devServices(config);

        // Create and configure Pub/Sub emulator container
        FirebaseEmulatorContainer emulatorContainer = new FirebaseEmulatorContainer(
                config.firebase().devservice(),
                devServices,
                config.projectId());

        // Set container startup timeout if provided
        timeout.ifPresent(emulatorContainer::withStartupTimeout);
        emulatorContainer.start();

        // Set the config for the started container
        FirebaseDevServiceProcessor.config = config;

        var emulatorContainerConfig = emulatorContainer.config();

        if (LOGGER.isInfoEnabled()) {
            var runningPorts = emulatorContainer.emulatorPorts();
            runningPorts.forEach((e, p) -> {
                LOGGER.info("Google Cloude Emulator " + e + " reachable on port " + p);
            });

            emulatorContainerConfig.forEach((e, h) -> {
                LOGGER.info("Google Cloud emulator config property " + e + " set to " + h);
            });
        }

        // Return running service item with container details
        return new DevServicesResultBuildItem.RunningDevService(FirebaseBuildSteps.FEATURE,
                emulatorContainer.getContainerId(),
                emulatorContainer::close,
                emulatorContainerConfig);
    }

    /**
     * Stops the running Firebase emulator container.
     */
    private void stopContainer() {
        if (devService != null && devService.isOwner()) {
            try {
                // Try closing the running dev service
                devService.close();
            } catch (Throwable e) {
                LOGGER.error("Failed to stop firebase container", e);
            } finally {
                devService = null;
            }
        }
    }

    public static class FirebaseEmulatorContainer extends GenericContainer<FirebaseEmulatorContainer> {

        public enum Emulators {
            AUTHENTICATION(
                    9099,
                    "quarkus.google.cloud.firebase.auth.emulator-host",
                    "auth"),
            EMULATOR_SUITE_UI(
                    4000,
                    "quarkus.google.cloud.firebase.emulator-host",
                    "ui"),
            CLOUD_FUNCTIONS(
                    5001,
                    "quarkus.google.cloud.functions.emulator-host",
                    "functions"),
            EVENT_ARC(
                    9299,
                    "quarkus.google.cloud.eventarc.emulator-host",
                    "eventarc"),
            REALTIME_DATABASE(
                    9000,
                    "quarkus.google.cloud.realtimedb.emulator-host",
                    "database"),
            CLOUD_FIRESTORE(
                    8080,
                    "quarkus.google.cloud.firestore.emulator-host",
                    "firestore"),
            CLOUD_STORAGE(
                    9199,
                    "quarkus.google.cloud.storage.emulator-host",
                    "storage"),
            FIREBASE_HOSTING(
                    5000,
                    "quarkus.google.cloud.firebase.hosting.emulator-host",
                    "hosting"),
            PUB_SUB(
                    8085,
                    "quarkus.google.cloud.pubsub.emulator-host",
                    "pubsub");

            final int internalPort;
            final String configProperty;
            final String emulatorName;

            Emulators(int internalPort, String configProperty, String onlyArgument) {
                this.internalPort = internalPort;
                this.configProperty = configProperty;
                this.emulatorName = onlyArgument;
            }

            public String getConfigProperty() {
                return configProperty;
            }

            public String getEmulatorName() {
                return emulatorName;
            }
        }

        private final Map<Emulators, FirebaseDevServiceConfig.GenericDevService> devServices;

        public FirebaseEmulatorContainer(FirebaseDevServiceConfig.Firebase.DevService firebaseConfig,
                Map<Emulators, FirebaseDevServiceConfig.GenericDevService> devServices,
                Optional<String> projectId) {
            super(new FirebaseDockerBuilder(
                    firebaseConfig,
                    devServices,
                    projectId).build());

            firebaseConfig.emulatorData().ifPresent(path -> {
                // TODO: https://firebase.google.com/docs/emulator-suite/install_and_configure#export_and_import_emulator_data
                // Mount the volume to the specified path
            });

            this.devServices = devServices;
        }

        private static class FirebaseDockerBuilder {

            private final ImageFromDockerfile result;

            private final FirebaseDevServiceConfig.Firebase.DevService firebaseConfig;
            private final Map<Emulators, FirebaseDevServiceConfig.GenericDevService> devServices;
            private final Optional<String> projectId;

            private DockerfileBuilder dockerBuilder;

            public FirebaseDockerBuilder(FirebaseDevServiceConfig.Firebase.DevService firebaseConfig,
                    Map<Emulators, FirebaseDevServiceConfig.GenericDevService> devServices,
                    Optional<String> projectId) {
                this.projectId = projectId;
                this.devServices = devServices;
                this.firebaseConfig = firebaseConfig;

                this.result = new ImageFromDockerfile()
                        .withDockerfileFromBuilder(builder -> {
                            this.dockerBuilder = builder;
                        });
            }

            public ImageFromDockerfile build() {
                this.validateConfiguration();
                this.configureBaseImage();
                this.installNeededSoftware();
                this.authenticateToFirebase();
                this.setupJavaToolOptions();
                this.addFirebaseJson();
                this.setupDataImportExport();
                this.runExecutable();

                return result;
            }

            private void validateConfiguration() {
                if (isEmulatorEnabled(devServices, Emulators.AUTHENTICATION) && projectId.isEmpty()) {
                    throw new IllegalStateException("Can't create Firebase Auth emulator. Google Project id is required");
                }

                // TODO: Validate if a custom firebase.json is defined, that the hosts are defined as 0.0.0.0
            }

            private void configureBaseImage() {
                dockerBuilder.from(firebaseConfig.imageName());
            }

            private void installNeededSoftware() {
                dockerBuilder
                        .run("apk --no-cache add openjdk11-jre bash curl openssl gettext nano nginx sudo")
                        .run("npm cache clean --force")
                        .run("npm i -g firebase-tools@" + firebaseConfig.firebaseVersion());
            }

            private void authenticateToFirebase() {
                firebaseConfig.token().ifPresent(token ->
                        dockerBuilder.env("FIREBASE_TOKEN", token));
            }

            private void setupJavaToolOptions() {
                firebaseConfig.javaToolOptions().ifPresent( toolOptions ->
                        dockerBuilder.env("JAVA_TOOL_OPTIONS", toolOptions));
            }

            private void addFirebaseJson() {
                dockerBuilder.workDir("/srv/firebase");

                firebaseConfig.customFirebaseJson().ifPresentOrElse(
                        this::includeCustomFirebaseJson,
                        this::generateFirebaseJson
                );

                this.dockerBuilder.add("firebase.json", "/srv/firebase/firebase.json");
            }

            private void includeCustomFirebaseJson(String customFilePath) {
                this.result.withFileFromPath(
                        "firebase.json",
                        new File(customFilePath).toPath()
                );
            }

            private void generateFirebaseJson() {
                StringBuilder firebaseJson = new StringBuilder();

                firebaseJson.append("{\n");
                firebaseJson.append("\t\"emulators\": {\n");

                var emulatorsJson = this.devServices
                        .keySet()
                        .stream()
                        .filter(e -> isEmulatorEnabled(devServices, e))
                        .map((emulator ->
                                "\t\t\"" + emulator.emulatorName + "\": {\n" +
                                "\t\t\t\"port\": " + emulator.internalPort + ",\n" +
                                "\t\t\t\"host\": \"0.0.0.0\"\n" +
                                "\t\t}"))
                        .collect(Collectors.joining(",\n"));
                firebaseJson.append(emulatorsJson).append("\n");

                firebaseJson.append("\t}\n");
                firebaseJson.append("}\n");

                this.result.withFileFromString("firebase.json", firebaseJson.toString());
            }

            private void setupDataImportExport() {
                firebaseConfig.emulatorData().ifPresent(emulator -> {
                    this.dockerBuilder.run("mkdir -p /srv/firebase/data");
                    this.dockerBuilder.volume("/srv/firebase/data");
                });
            }

            private void runExecutable() {
                var projectArgument = projectId
                        .map(id -> " --project " + id)
                        .orElse("");

                var importArgument = firebaseConfig
                        .emulatorData()
                        .map( path -> " --import=/srv/firebase/data --export-on-exit")
                        .orElse("");

                dockerBuilder
                        .cmd("firebase emulators:start" + projectArgument + importArgument);
            }
        }

        /**
         * Configures the Pub/Sub emulator container.
         */
        @Override
        public void configure() {
            super.configure();

            enabledEmulators()
                    .forEach(emulator -> {
                        var fixedExposedPort = devServices.get(emulator).emulatorPort();
                        // Expose emulatorPort
                        if (fixedExposedPort.isPresent()) {
                            addFixedExposedPort(fixedExposedPort.get(), emulator.internalPort);
                        } else {
                            addExposedPort(emulator.internalPort);
                        }
                    });
        }

        public Map<String, String> config() {
            return enabledEmulators()
                    .collect(Collectors.toMap(
                            Emulators::getConfigProperty,
                            this::getEmulatorEndpoint));

        }

        public Map<Emulators, Integer> emulatorPorts() {
            return enabledEmulators()
                    .collect(Collectors.toMap(
                            e -> e,
                            e -> this.getMappedPort(e.internalPort)));
        }

        private Stream<Emulators> enabledEmulators() {
            return Arrays.stream(Emulators.values())
                    .filter(this::isEmulatorEnabled);
        }

        private boolean isEmulatorEnabled(Emulators emulator) {
            return isEmulatorEnabled(this.devServices, emulator);
        }

        private static boolean isEmulatorEnabled(Map<Emulators, FirebaseDevServiceConfig.GenericDevService> devServices,
                Emulators emulator) {
            return Optional.ofNullable(devServices.get(emulator))
                    .map(FirebaseDevServiceConfig.GenericDevService::enabled)
                    .orElse(false);
        }

        private String getEmulatorEndpoint(Emulators emulator) {
            return this.getHost() + ":" + this.getMappedPort(emulator.internalPort);
        }
    }

}
