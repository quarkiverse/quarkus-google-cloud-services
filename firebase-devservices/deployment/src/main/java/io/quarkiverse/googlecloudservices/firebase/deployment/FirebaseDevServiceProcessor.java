package io.quarkiverse.googlecloudservices.firebase.deployment;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

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

    @BuildStep
    public DevServicesResultBuildItem start(DockerStatusBuildItem dockerStatusBuildItem,
            FirebaseDevServiceProjectConfig projectConfig,
            FirebaseDevServiceConfig firebaseBuildTimeConfig,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LaunchModeBuildItem launchMode,
            LoggingSetupBuildItem loggingSetupBuildItem,
            BuildProducer<CardPageBuildItem> cardProducer,
            DevServicesConfig globalDevServicesConfig) {
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
            devService = startContainerIfAvailable(
                    dockerStatusBuildItem,
                    closeBuildItem,
                    projectConfig,
                    firebaseBuildTimeConfig,
                    globalDevServicesConfig.timeout());
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
                    .staticLabel(firebaseBuildTimeConfig.firebase().emulator().firebaseVersion())
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
     * @param timeout, Optional timeout for starting the service
     * @param closeBuildItem The close build item
     * @return Running service item, or null if the service couldn't be started
     */
    private DevServicesResultBuildItem.RunningDevService startContainerIfAvailable(DockerStatusBuildItem dockerStatusBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            FirebaseDevServiceProjectConfig projectConfig,
            FirebaseDevServiceConfig config,
            Optional<Duration> timeout) {

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

        return startContainer(closeBuildItem, projectConfig, config, timeout);
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
     * @return Running service item, or null if the service couldn't be started
     */
    private DevServicesResultBuildItem.RunningDevService startContainer(CuratedApplicationShutdownBuildItem closeBuildItem,
            FirebaseDevServiceProjectConfig projectConfig,
            FirebaseDevServiceConfig config,
            Optional<Duration> timeout) {

        // Create and configure Firebase emulator container
        var emulatorContainer = new FirebaseEmulatorConfigBuilder(projectConfig, config).build();

        // Set container startup timeout if provided
        timeout.ifPresent(emulatorContainer::withStartupTimeout);
        emulatorContainer.start();

        // Set the config for the started container
        FirebaseDevServiceProcessor.config = config;

        var emulatorContainerConfig = emulatorContainerConfig(emulatorContainer);

        if (LOGGER.isInfoEnabled()) {
            var runningPorts = emulatorContainer.emulatorPorts();
            runningPorts.forEach((e, p) -> LOGGER.info("Google Cloud Emulator " + e + " reachable on port " + p));

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

    private Map<String, String> emulatorContainerConfig(FirebaseEmulatorContainer emulatorContainer) {
        return emulatorContainer.emulatorEndpoints()
                .entrySet()
                .stream()
                .filter(e -> CONFIG_PROPERTIES.containsKey(e.getKey()))
                .collect(
                        Collectors.toMap(
                                e -> configPropertyForEmulator(e.getKey()),
                                Map.Entry::getValue));
    }

    private String configPropertyForEmulator(FirebaseEmulatorContainer.Emulator emulator) {
        return CONFIG_PROPERTIES.get(emulator);
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
