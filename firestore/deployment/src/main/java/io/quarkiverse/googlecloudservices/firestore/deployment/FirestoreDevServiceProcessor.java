package io.quarkiverse.googlecloudservices.firestore.deployment;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;
import org.testcontainers.containers.FirestoreEmulatorContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.*;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.devservices.common.ConfigureUtil;

/**
 * Processor responsible for managing Firestore Dev Services.
 * <p>
 * The processor starts the Firestore service in case it's not running.
 */
@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = DevServicesConfig.Enabled.class)
public class FirestoreDevServiceProcessor {

    private static final Logger LOGGER = Logger.getLogger(FirestoreDevServiceProcessor.class.getName());

    // Running dev service instance
    private static volatile DevServicesResultBuildItem.RunningDevService devService;
    // Configuration for the Firestore Dev service
    private static volatile FirestoreDevServiceConfig config;

    @BuildStep
    public DevServicesResultBuildItem start(
            DockerStatusBuildItem dockerStatusBuildItem,
            FirestoreBuildTimeConfig buildTimeConfig,
            FirebaseDevServiceConfig firebaseConfig,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LaunchModeBuildItem launchMode,
            LoggingSetupBuildItem loggingSetupBuildItem,
            DevServicesConfig devServicesConfig) {
        // If dev service is running and config has changed, stop the service
        if (devService != null && !buildTimeConfig.devservice().equals(config)) {
            stopContainer();
        } else if (devService != null) {
            return devService.toBuildItem();
        }

        // Set up log compressor for startup logs
        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Google Cloud Firestore Dev Services Starting:",
                consoleInstalledBuildItem,
                loggingSetupBuildItem);

        // Try starting the container if conditions are met
        try {
            boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                    devServicesSharedNetworkBuildItem);
            devService = startContainerIfAvailable(dockerStatusBuildItem, buildTimeConfig.devservice(), firebaseConfig,
                    devServicesConfig.timeout(), composeProjectBuildItem, useSharedNetwork);
        } catch (Throwable t) {
            LOGGER.warn("Unable to start Firestore dev service", t);
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
     * @param config, Configuration for the Firestore service
     * @param timeout, Optional timeout for starting the service
     * @param composeProjectBuildItem The compose build item
     * @param useSharedNetwork Start the service on a shared docker network
     * @return Running service item, or null if the service couldn't be started
     */
    private DevServicesResultBuildItem.RunningDevService startContainerIfAvailable(
            DockerStatusBuildItem dockerStatusBuildItem,
            FirestoreDevServiceConfig config,
            FirebaseDevServiceConfig firebaseConfig,
            Optional<Duration> timeout,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            boolean useSharedNetwork) {
        if (!config.enabled()) {
            // Firestore service explicitly disabled
            LOGGER.debug("Not starting Dev Services for Firestore as it has been disabled in the config");
            return null;
        }

        if (firebaseConfig.preferFirebaseDevServices().orElse(false)) {
            // Firebase DevServices are included, use them instead
            LOGGER.debug("Not starting Dev Services for Firestore as the Firebase DevServices are preferred");
            return null;
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            LOGGER.warn("Not starting devservice because docker is not available");
            return null;
        }

        return startContainer(dockerStatusBuildItem, config, timeout, composeProjectBuildItem, useSharedNetwork);
    }

    /**
     * Starts the Firestore emulator container with provided configuration.
     *
     * @param dockerStatusBuildItem, Docker status
     * @param config, Configuration for the Firestore service
     * @param timeout, Optional timeout for starting the service
     * @param composeProjectBuildItem The compose build item
     * @param useSharedNetwork Start the service on a shared docker network
     * @return Running service item, or null if the service couldn't be started
     */
    private DevServicesResultBuildItem.RunningDevService startContainer(
            DockerStatusBuildItem dockerStatusBuildItem,
            FirestoreDevServiceConfig config,
            Optional<Duration> timeout,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            boolean useSharedNetwork) {
        // Create and configure Firestore emulator container
        FirestoreEmulatorContainer emulatorContainer = new QuarkusFirestoreContainer(
                DockerImageName.parse(config.imageName())
                        .asCompatibleSubstituteFor("gcr.io/google.com/cloudsdktool/cloud-sdk:emulators"),
                config.emulatorPort().orElse(null),
                composeProjectBuildItem.getDefaultNetworkId(),
                useSharedNetwork);

        // Set container startup timeout if provided
        timeout.ifPresent(emulatorContainer::withStartupTimeout);
        emulatorContainer.start();

        // Set the config for the started container
        FirestoreDevServiceProcessor.config = config;

        // Return running service item with container details
        return new DevServicesResultBuildItem.RunningDevService(FirestoreBuildSteps.FEATURE,
                emulatorContainer.getContainerId(),
                emulatorContainer::close, "quarkus.google.cloud.firestore.host-override",
                emulatorContainer.getEmulatorEndpoint());
    }

    /**
     * Stops the running Firestore emulator container.
     */
    private void stopContainer() {
        if (devService != null && devService.isOwner()) {
            try {
                // Try closing the running dev service
                devService.close();
            } catch (Throwable e) {
                LOGGER.error("Failed to stop firestore container", e);
            } finally {
                devService = null;
            }
        }
    }

    /**
     * Class for creating and configuring a Firestore emulator container.
     */
    private static class QuarkusFirestoreContainer extends FirestoreEmulatorContainer {

        private final Integer fixedExposedPort;
        private final boolean useSharedNetwork;
        private final String hostName;
        private static final int INTERNAL_PORT = 8080;

        private QuarkusFirestoreContainer(DockerImageName dockerImageName, Integer fixedExposedPort,
                String defaultNetworkId, boolean useSharedNetwork) {
            super(dockerImageName);
            this.fixedExposedPort = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;
            this.hostName = ConfigureUtil.configureNetwork(this, defaultNetworkId, useSharedNetwork, "firestore");
        }

        /**
         * Configures the Firestore emulator container.
         */
        @Override
        public void configure() {
            super.configure();
            if (useSharedNetwork) {
                return;
            }

            // Expose Firestore emulatorPort
            if (fixedExposedPort != null) {
                addFixedExposedPort(fixedExposedPort, INTERNAL_PORT);
            } else {
                addExposedPort(INTERNAL_PORT);
            }
        }

        @Override
        public String getEmulatorEndpoint() {
            if (useSharedNetwork) {
                return hostName + ":" + INTERNAL_PORT;
            } else {
                return super.getEmulatorEndpoint();
            }
        }
    }
}
