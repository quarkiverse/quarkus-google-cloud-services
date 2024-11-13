package io.quarkiverse.googlecloudservices.firebase.deployment;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

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

    private static final Map<FirebaseEmulatorContainer.Emulators, String> CONFIG_PROPERTIES = Map.of(
            FirebaseEmulatorContainer.Emulators.AUTHENTICATION, "quarkus.google.cloud.firebase.auth.emulator-host",
            FirebaseEmulatorContainer.Emulators.EMULATOR_SUITE_UI, "quarkus.google.cloud.firebase.emulator-host",
            FirebaseEmulatorContainer.Emulators.FIREBASE_HOSTING, "quarkus.google.cloud.firebase.hosting.emulator-host",
            FirebaseEmulatorContainer.Emulators.CLOUD_FUNCTIONS, "quarkus.google.cloud.functions.emulator-host",
            FirebaseEmulatorContainer.Emulators.EVENT_ARC, "quarkus.google.cloud.eventarc.emulator-host",
            FirebaseEmulatorContainer.Emulators.REALTIME_DATABASE, "quarkus.google.cloud.database.emulator-host",
            FirebaseEmulatorContainer.Emulators.CLOUD_FIRESTORE, "quarkus.google.cloud.firestore.host-override",
            FirebaseEmulatorContainer.Emulators.CLOUD_STORAGE, "quarkus.google.cloud.storage.host-override",
            FirebaseEmulatorContainer.Emulators.PUB_SUB, "quarkus.google.cloud.pubsub.emulator-host");

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
            LOGGER.info("Not starting DevService because docker is not available");
            return null;
        }

        return startContainer(dockerStatusBuildItem, config, timeout);
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
     * @param dockerStatusBuildItem, Docker status
     * @param config, Configuration for the Firebase service
     * @param timeout, Optional timeout for starting the service
     * @return Running service item, or null if the service couldn't be started
     */
    private DevServicesResultBuildItem.RunningDevService startContainer(DockerStatusBuildItem dockerStatusBuildItem,
            FirebaseDevServiceConfig config,
            Optional<Duration> timeout) {

        var emulatorConfig = new FirebaseEmulatorConfigBuilder(config).build();

        // Create and configure Pub/Sub emulator container
        FirebaseEmulatorContainer emulatorContainer = new FirebaseEmulatorContainer(emulatorConfig);

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

    private String configPropertyForEmulator(FirebaseEmulatorContainer.Emulators emulator) {
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
