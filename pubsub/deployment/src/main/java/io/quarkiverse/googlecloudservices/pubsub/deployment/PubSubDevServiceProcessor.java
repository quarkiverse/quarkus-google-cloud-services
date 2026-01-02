package io.quarkiverse.googlecloudservices.pubsub.deployment;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;
import org.testcontainers.containers.PubSubEmulatorContainer;
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
 * Processor responsible for managing Pub/Sub Dev Services.
 * <p>
 * The processor starts the Pub/Sub service in case it's not running.
 */
@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = DevServicesConfig.Enabled.class)
public class PubSubDevServiceProcessor {

    private static final Logger LOGGER = Logger.getLogger(PubSubDevServiceProcessor.class.getName());

    // Running dev service instance
    private static volatile DevServicesResultBuildItem.RunningDevService devService;
    // Configuration for the Pub/Sub Dev service
    private static volatile PubSubDevServiceConfig config;

    @BuildStep
    public DevServicesResultBuildItem start(
            DockerStatusBuildItem dockerStatusBuildItem,
            PubSubDevServiceConfig devServiceConfig,
            FirebaseDevServiceConfig firebaseConfig,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LaunchModeBuildItem launchMode,
            LoggingSetupBuildItem loggingSetupBuildItem,
            DevServicesConfig devServicesConfig) {
        // If dev service is running and config has changed, stop the service
        if (devService != null && !devServiceConfig.equals(config)) {
            stopContainer();
        } else if (devService != null) {
            return devService.toBuildItem();
        }

        // Set up log compressor for startup logs
        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Google Cloud PubSub Dev Services Starting:",
                consoleInstalledBuildItem,
                loggingSetupBuildItem);

        // Try starting the container if conditions are met
        try {
            boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                    devServicesSharedNetworkBuildItem);
            devService = startContainerIfAvailable(dockerStatusBuildItem, devServiceConfig,
                    firebaseConfig, devServicesConfig.timeout(), composeProjectBuildItem, useSharedNetwork);
        } catch (Throwable t) {
            LOGGER.warn("Unable to start PubSub dev service", t);
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
     * @param config, Configuration for the Pub/Sub service
     * @param timeout, Optional timeout for starting the service
     * @param composeProjectBuildItem The compose build item
     * @param useSharedNetwork Start the service on a shared docker network
     * @return Running service item, or null if the service couldn't be started
     */
    private DevServicesResultBuildItem.RunningDevService startContainerIfAvailable(
            DockerStatusBuildItem dockerStatusBuildItem,
            PubSubDevServiceConfig config,
            FirebaseDevServiceConfig firebaseConfig,
            Optional<Duration> timeout,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            boolean useSharedNetwork) {
        if (!config.enabled()) {
            // PubSub service explicitly disabled
            LOGGER.debug("Not starting Dev Services for PubSub as it has been disabled in the config");
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
     * Starts the Pub/Sub emulator container with provided configuration.
     *
     * @param dockerStatusBuildItem, Docker status
     * @param config, Configuration for the PubSub service
     * @param timeout, Optional timeout for starting the service
     * @param composeProjectBuildItem The compose build item
     * @param useSharedNetwork Start the service on a shared docker network
     * @return Running service item, or null if the service couldn't be started
     */
    private DevServicesResultBuildItem.RunningDevService startContainer(
            DockerStatusBuildItem dockerStatusBuildItem,
            PubSubDevServiceConfig config,
            Optional<Duration> timeout,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            boolean useSharedNetwork) {
        // Create and configure Pub/Sub emulator container
        PubSubEmulatorContainer emulatorContainer = new QuarkusPubSubContainer(
                DockerImageName.parse(config.imageName())
                        .asCompatibleSubstituteFor("gcr.io/google.com/cloudsdktool/cloud-sdk:emulators"),
                config.emulatorPort().orElse(null),
                composeProjectBuildItem.getDefaultNetworkId(),
                useSharedNetwork);

        // Set container startup timeout if provided
        timeout.ifPresent(emulatorContainer::withStartupTimeout);
        emulatorContainer.start();

        // Set the config for the started container
        PubSubDevServiceProcessor.config = config;

        // Return running service item with container details
        return new DevServicesResultBuildItem.RunningDevService(PubSubBuildSteps.FEATURE,
                emulatorContainer.getContainerId(),
                emulatorContainer::close, "quarkus.google.cloud.pubsub.emulator-host",
                emulatorContainer.getEmulatorEndpoint());
    }

    /**
     * Stops the running Pub/Sub emulator container.
     */
    private void stopContainer() {
        if (devService != null && devService.isOwner()) {
            try {
                // Try closing the running dev service
                devService.close();
            } catch (Throwable e) {
                LOGGER.error("Failed to stop pubsub container", e);
            } finally {
                devService = null;
            }
        }
    }

    /**
     * Class for creating and configuring a PubSub emulator container.
     */
    private static class QuarkusPubSubContainer extends PubSubEmulatorContainer {

        private final Integer fixedExposedPort;
        private final boolean useSharedNetwork;
        private final String hostName;
        private static final int INTERNAL_PORT = 8085;

        private QuarkusPubSubContainer(DockerImageName dockerImageName, Integer fixedExposedPort,
                String defaultNetworkId, boolean useSharedNetwork) {
            super(dockerImageName);
            this.fixedExposedPort = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;
            this.hostName = ConfigureUtil.configureNetwork(this, defaultNetworkId, useSharedNetwork, "pubsub");
        }

        /**
         * Configures the Pub/Sub emulator container.
         */
        @Override
        public void configure() {
            super.configure();
            if (useSharedNetwork) {
                return;
            }

            // Expose Pub/Sub emulatorPort
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
