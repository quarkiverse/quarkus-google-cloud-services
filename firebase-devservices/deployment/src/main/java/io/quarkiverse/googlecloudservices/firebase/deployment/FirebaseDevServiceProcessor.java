package io.quarkiverse.googlecloudservices.firebase.deployment;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.testcontainers.Testcontainers;

import io.quarkiverse.googlecloudservices.firebase.deployment.testcontainers.FirebaseEmulatorContainer;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.*;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.ExternalPageBuilder;
import io.quarkus.devui.spi.page.Page;

/**
 * Processor responsible for managing Firebase Dev Services.
 * <p>
 * The processor starts the Firebase service in case it's not running.
 */
@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = DevServicesConfig.Enabled.class)
public class FirebaseDevServiceProcessor {

    private static final Logger LOGGER = Logger.getLogger(FirebaseDevServiceProcessor.class.getName());

    // Running dev service instance
    private static volatile DevServicesResultBuildItem.RunningDevService devService;
    // Configuration for the Firebase Dev service
    private static volatile FirebaseDevServiceConfig config;

    private static final Map<FirebaseEmulatorContainer.Emulator, String> CONFIG_PROPERTIES = Map.of(
            FirebaseEmulatorContainer.Emulator.AUTHENTICATION, "quarkus.google.cloud.firebase.auth.emulator-host",
            FirebaseEmulatorContainer.Emulator.EMULATOR_SUITE_UI, "quarkus.google.cloud.firebase.emulator-host",
            FirebaseEmulatorContainer.Emulator.FIREBASE_HOSTING, "quarkus.google.cloud.firebase.hosting.emulator-host",
            FirebaseEmulatorContainer.Emulator.CLOUD_FUNCTIONS, "quarkus.google.cloud.functions.emulator-host",
            FirebaseEmulatorContainer.Emulator.EVENT_ARC, "quarkus.google.cloud.eventarc.emulator-host",
            FirebaseEmulatorContainer.Emulator.REALTIME_DATABASE, "quarkus.google.cloud.firebase.database.host-override",
            FirebaseEmulatorContainer.Emulator.CLOUD_FIRESTORE, "quarkus.google.cloud.firestore.host-override",
            FirebaseEmulatorContainer.Emulator.CLOUD_STORAGE, "quarkus.google.cloud.storage.host-override",
            FirebaseEmulatorContainer.Emulator.PUB_SUB, "quarkus.google.cloud.pubsub.emulator-host");

    // Additional config properties exposing the emulator endpoints as reachable from any other container
    // started by Testcontainers in this JVM (e.g. the Playwright browser container, or a containerized
    // app-under-test), via Testcontainers.exposeHostPorts() + the "host.testcontainers.internal" ambassador
    // hostname. A container started outside Testcontainers (e.g. via docker-compose) won't have that ambassador
    // wired up and must instead use the shared-network alias (see FirebaseEmulatorContainer#emulatorUrl).
    private static final Map<FirebaseEmulatorContainer.Emulator, String> CONTAINER_CONFIG_PROPERTIES = Map.of(
            FirebaseEmulatorContainer.Emulator.AUTHENTICATION, "quarkus.google.cloud.firebase.auth.container-emulator-host",
            FirebaseEmulatorContainer.Emulator.EMULATOR_SUITE_UI, "quarkus.google.cloud.firebase.container-emulator-host",
            FirebaseEmulatorContainer.Emulator.FIREBASE_HOSTING,
            "quarkus.google.cloud.firebase.hosting.container-emulator-host",
            FirebaseEmulatorContainer.Emulator.CLOUD_FUNCTIONS, "quarkus.google.cloud.functions.container-emulator-host",
            FirebaseEmulatorContainer.Emulator.EVENT_ARC, "quarkus.google.cloud.eventarc.container-emulator-host",
            FirebaseEmulatorContainer.Emulator.REALTIME_DATABASE,
            "quarkus.google.cloud.firebase.database.container-host-override",
            FirebaseEmulatorContainer.Emulator.CLOUD_FIRESTORE, "quarkus.google.cloud.firestore.container-host-override",
            FirebaseEmulatorContainer.Emulator.CLOUD_STORAGE, "quarkus.google.cloud.storage.container-host-override",
            FirebaseEmulatorContainer.Emulator.PUB_SUB, "quarkus.google.cloud.pubsub.container-emulator-host");

    private static final String HOST_TESTCONTAINERS_INTERNAL = "host.testcontainers.internal";

    @BuildStep
    public DevServicesResultBuildItem start(
            DockerStatusBuildItem dockerStatusBuildItem,
            FirebaseDevServiceProjectConfig projectConfig,
            FirebaseDevServiceConfig firebaseBuildTimeConfig,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LaunchModeBuildItem launchMode,
            LoggingSetupBuildItem loggingSetupBuildItem,
            BuildProducer<CardPageBuildItem> cardProducer,
            DevServicesConfig devServicesConfig) {
        // If dev service is running and config has changed, stop the service
        if (devService != null && !firebaseBuildTimeConfig.equals(config)) {
            stopContainer();
        } else if (devService != null) {
            createDevServiceCard(devService, firebaseBuildTimeConfig, launchMode, cardProducer);
            return devService.toBuildItem();
        }

        // Set up log compressor for startup logs
        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Google Cloud Firebase Dev Services Starting:",
                consoleInstalledBuildItem,
                loggingSetupBuildItem);

        // Try starting the container if conditions are met
        try {
            boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                    devServicesSharedNetworkBuildItem);
            devService = startContainerIfAvailable(
                    dockerStatusBuildItem,
                    closeBuildItem,
                    projectConfig,
                    firebaseBuildTimeConfig,
                    devServicesConfig.timeout(),
                    composeProjectBuildItem,
                    useSharedNetwork);
        } catch (Throwable t) {
            LOGGER.warn("Unable to start Firebase dev service", t);
            // Dump captured logs in case of an error
            compressor.closeAndDumpCaptured();
            return null;
        } finally {
            compressor.close();
        }

        createDevServiceCard(devService, firebaseBuildTimeConfig, launchMode, cardProducer);
        return devService == null ? null : devService.toBuildItem();
    }

    private void createDevServiceCard(DevServicesResultBuildItem.RunningDevService devService,
            FirebaseDevServiceConfig firebaseBuildTimeConfig,
            LaunchModeBuildItem launchMode,
            BuildProducer<CardPageBuildItem> cardProducer) {
        if (launchMode.isNotLocalDevModeType()) {
            return;
        }

        var config = devService.getConfig();

        var cardBuildItem = new CardPageBuildItem();
        cardBuildItem.addBuildTimeData("emulators", config
                .entrySet()
                .stream()
                .map(entry -> {
                    var emulator = CONFIG_PROPERTIES.entrySet()
                            .stream()
                            .filter(e -> e.getValue().equals(entry.getKey()))
                            .findFirst()
                            .map(Map.Entry::getKey)
                            .orElse(null);

                    return new EmulatorRow(emulator, entry.getKey(), entry.getValue());
                })
                .toList());

        cardBuildItem.addPage(Page.tableDataPageBuilder("Running emulators")
                .showColumn("name")
                .showColumn("configProperty")
                .showColumn("host")
                .icon("font-awesome-solid:plug")
                .staticLabel("" + config.size())
                .buildTimeDataKey("emulators"));

        var uiHost = config.get(CONFIG_PROPERTIES.get(FirebaseEmulatorContainer.Emulator.EMULATOR_SUITE_UI));
        if (uiHost != null) {
            cardBuildItem.addPage(Page.externalPageBuilder("Firebase UI")
                    .url(uiHost, uiHost)
                    .icon("font-awesome-solid:gauge-high")
                    .staticLabel(firebaseBuildTimeConfig.firebase().emulator().firebaseVersion().orElse("auto-detected"))
                    .mimeType(ExternalPageBuilder.MIME_TYPE_HTML));
        }

        cardProducer.produce(cardBuildItem);
    }

    public static class EmulatorRow {
        private final FirebaseEmulatorContainer.Emulator name;
        private final String configProperty;
        private final String host;

        public EmulatorRow(FirebaseEmulatorContainer.Emulator name, String configProperty, String host) {
            this.name = name;
            this.configProperty = configProperty;
            this.host = host;
        }

        public FirebaseEmulatorContainer.Emulator getName() {
            return name;
        }

        public String getConfigProperty() {
            return configProperty;
        }

        public String getHost() {
            return host;
        }
    }

    /**
     * Start the container if conditions are met.
     *
     * @param dockerStatusBuildItem, Docker status
     * @param config, Configuration for the Firebase service
     * @param closeBuildItem The close build item
     * @param projectConfig The project configuration
     * @param timeout, Optional timeout for starting the service
     * @param useSharedNetwork Start the service on a shared docker network
     * @return Running service item, or null if the service couldn't be started
     */
    private DevServicesResultBuildItem.RunningDevService startContainerIfAvailable(
            DockerStatusBuildItem dockerStatusBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            FirebaseDevServiceProjectConfig projectConfig,
            FirebaseDevServiceConfig config,
            Optional<Duration> timeout,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            boolean useSharedNetwork) {

        if (!config.firebase().preferFirebaseDevServices()) {
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
            LOGGER.info("Not starting DevService because docker is not available");
            return null;
        }

        return startContainer(closeBuildItem, projectConfig, config, timeout, composeProjectBuildItem, useSharedNetwork);
    }

    private boolean isEnabled(FirebaseDevServiceConfig config) {
        return FirebaseEmulatorConfigBuilder.devServices(config)
                .values()
                .stream()
                .map(FirebaseDevServiceConfig.GenericDevService::enabled)
                .reduce(Boolean.FALSE, Boolean::logicalOr);
    }

    /**
     * Starts the Pub/Sub emulator container with provided configuration.
     *
     * @param closeBuildItem The close build item to handle shutdown of the container
     * @param config, Configuration for the Firebase service
     * @param timeout, Optional timeout for starting the service
     * @param useSharedNetwork Start the service on a shared docker network
     * @return Running service item, or null if the service couldn't be started
     */
    private DevServicesResultBuildItem.RunningDevService startContainer(
            CuratedApplicationShutdownBuildItem closeBuildItem,
            FirebaseDevServiceProjectConfig projectConfig,
            FirebaseDevServiceConfig config,
            Optional<Duration> timeout,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            boolean useSharedNetwork) {

        // Create and configure Firebase emulator container
        var emulatorContainer = new FirebaseEmulatorConfigBuilder(projectConfig, config, useSharedNetwork).build();
        String hostName = ConfigureUtil.configureNetwork(
                emulatorContainer,
                composeProjectBuildItem.getDefaultNetworkId(),
                useSharedNetwork,
                "firebase");

        // Set container startup timeout if provided
        timeout.ifPresent(emulatorContainer::withStartupTimeout);
        emulatorContainer.setupSharedNetworkHost(hostName);
        emulatorContainer.start();

        // Set the config for the started container
        FirebaseDevServiceProcessor.config = config;

        var emulatorContainerConfig = emulatorContainerConfig(emulatorContainer, useSharedNetwork);

        if (LOGGER.isInfoEnabled()) {
            var runningPorts = emulatorContainer.emulatorUrls();
            runningPorts.forEach((e, p) -> LOGGER.info("Google Cloud Emulator " + e + " reachable on " + p));

            emulatorContainerConfig
                    .forEach((e, h) -> LOGGER.info("Google Cloud emulator config property " + e + " set to " + h));
        }

        closeBuildItem.addCloseTask(emulatorContainer::close, true);

        // Return running service item with container details
        return new DevServicesResultBuildItem.RunningDevService(FirebaseBuildSteps.FEATURE,
                emulatorContainer.getContainerId(),
                emulatorContainer::close,
                emulatorContainerConfig);
    }

    private Map<String, String> emulatorContainerConfig(FirebaseEmulatorContainer emulatorContainer, boolean useSharedNetwork) {
        var emulatorProperties = new HashMap<>(emulatorContainer.emulatorEndpoints()
                .entrySet()
                .stream()
                .filter(e -> CONFIG_PROPERTIES.containsKey(e.getKey()))
                .collect(
                        Collectors.toMap(
                                e -> configPropertyForEmulator(e.getKey()),
                                Map.Entry::getValue)));

        // In case either the pubsub or cloud firestore is running and we use shared network mode, we force the usage
        // of emulator credentials, as the default automatic detection won't work (because the hostname is set to the
        // shared network host instead of localhost).
        if (useSharedNetwork) {
            if (emulatorProperties.containsKey(CONFIG_PROPERTIES.get(FirebaseEmulatorContainer.Emulator.PUB_SUB))) {
                emulatorProperties.put("quarkus.google.cloud.pubsub.use-emulator-credentials", "true");
            }

            if (emulatorProperties.containsKey(CONFIG_PROPERTIES.get(FirebaseEmulatorContainer.Emulator.CLOUD_FIRESTORE))) {
                emulatorProperties.put("quarkus.google.cloud.firestore.use-emulator-credentials", "true");
            }

            // Expose the emulator's host-mapped ports to any container started by Testcontainers in this JVM
            // (e.g. the Playwright browser container), reachable via the "host.testcontainers.internal" ambassador.
            var configuredEmulators = emulatorContainer.hostEmulatorUrls().keySet();
            configuredEmulators
                    .forEach(emulator -> Testcontainers.exposeHostPorts(emulatorContainer.hostMappedPort(emulator)));

            emulatorProperties.putAll(configuredEmulators
                    .stream()
                    .filter(CONTAINER_CONFIG_PROPERTIES::containsKey)
                    .collect(Collectors.toMap(
                            CONTAINER_CONFIG_PROPERTIES::get,
                            emulator -> containerEmulatorUrl(emulatorContainer, emulator))));
        }

        return emulatorProperties;
    }

    private String configPropertyForEmulator(FirebaseEmulatorContainer.Emulator emulator) {
        return CONFIG_PROPERTIES.get(emulator);
    }

    /**
     * Build the URL on which an emulator is reachable from another Testcontainers-managed container, via the
     * {@code host.testcontainers.internal} ambassador (see {@link Testcontainers#exposeHostPorts(int...)}).
     */
    private String containerEmulatorUrl(FirebaseEmulatorContainer emulatorContainer,
            FirebaseEmulatorContainer.Emulator emulator) {
        return FirebaseEmulatorContainer.withHttpPrefixIfNeeded(emulator,
                HOST_TESTCONTAINERS_INTERNAL + ":" + emulatorContainer.hostMappedPort(emulator));
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

}
