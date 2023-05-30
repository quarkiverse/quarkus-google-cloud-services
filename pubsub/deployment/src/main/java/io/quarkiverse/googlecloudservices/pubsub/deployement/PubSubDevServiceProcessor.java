package io.quarkiverse.googlecloudservices.pubsub.deployement;

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
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;

/**
 * Processor responsible for managing PubSub Dev Services.
 * <p>
 * The processor starts the PubSub service in case it's not running.
 */
@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = GlobalDevServicesConfig.Enabled.class)
public class PubSubDevServiceProcessor {

    private static final Logger LOGGER = Logger.getLogger(PubSubDevServiceProcessor.class.getName());

    // Running dev service instance
    static volatile DevServicesResultBuildItem.RunningDevService devService;
    // Configuration for the PubSub Dev service
    static volatile PubSubDevServiceConfig config;

    @BuildStep
    public DevServicesResultBuildItem startPubSub(DockerStatusBuildItem dockerStatusBuildItem,
            PubSubBuildTimeConfig pubSubBuildTimeConfig,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LaunchModeBuildItem launchMode,
            LoggingSetupBuildItem loggingSetupBuildItem,
            GlobalDevServicesConfig globalDevServicesConfig) {
        // If dev service is running and config has changed, stop the service
        if (devService != null && !pubSubBuildTimeConfig.devservice.equals(config)) {
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
            devService = startContainerIfAvailable(dockerStatusBuildItem, pubSubBuildTimeConfig.devservice,
                    globalDevServicesConfig.timeout);
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
     * @param config, Configuration for the PubSub service
     * @param timeout, Optional timeout for starting the service
     * @return Running service item, or null if the service couldn't be started
     */
    private DevServicesResultBuildItem.RunningDevService startContainerIfAvailable(DockerStatusBuildItem dockerStatusBuildItem,
            PubSubDevServiceConfig config,
            Optional<Duration> timeout) {
        // Start container if PubSub is enabled and Docker is available
        if (config.enabled && dockerStatusBuildItem.isDockerAvailable()) {
            return startContainer(dockerStatusBuildItem, config, timeout);
        } else {
            LOGGER.warn(
                    "Not starting Dev Services for PubSub as it has been disabled in the config or Docker is not available");
            return null;
        }
    }

    /**
     * Starts the PubSub emulator container with provided configuration.
     *
     * @param dockerStatusBuildItem, Docker status
     * @param config, Configuration for the PubSub service
     * @param timeout, Optional timeout for starting the service
     * @return Running service item, or null if the service couldn't be started
     */
    private DevServicesResultBuildItem.RunningDevService startContainer(DockerStatusBuildItem dockerStatusBuildItem,
            PubSubDevServiceConfig config,
            Optional<Duration> timeout) {

        if (!config.enabled) {
            // PubSub service explicitly disabled
            LOGGER.debug("Not starting Dev Services for PubSub as it has been disabled in the config");
            return null;
        }

        if (!dockerStatusBuildItem.isDockerAvailable()) {
            LOGGER.warn("Not starting devservice because docker is not available");
            return null;
        }

        // Create and configure PubSub emulator container
        PubSubEmulatorContainer pubSubEmulatorContainer = new QuarkusPubSubContainer(
                DockerImageName.parse(config.imageName).asCompatibleSubstituteFor("gcr.io/google.com/cloudsdktool/cloud-sdk"),
                config.port);

        // Set container startup timeout if provided
        timeout.ifPresent(pubSubEmulatorContainer::withStartupTimeout);
        pubSubEmulatorContainer.start();

        // Return running service item with container details
        return new DevServicesResultBuildItem.RunningDevService(PubSubBuildSteps.FEATURE,
                pubSubEmulatorContainer.getContainerId(),
                pubSubEmulatorContainer::close, "pubsub", pubSubEmulatorContainer.getEmulatorEndpoint());
    }

    /**
     * Stops the running PubSub emulator container.
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
        private static final int PUBSUB_INTERNAL_PORT = 8085;

        private QuarkusPubSubContainer(DockerImageName dockerImageName, Integer fixedExposedPort) {
            super(dockerImageName);
            this.fixedExposedPort = fixedExposedPort;
        }

        /**
         * Configures the PubSub emulator container.
         */
        @Override
        public void configure() {
            super.configure();

            // Expose PubSub emulatorPort
            if (fixedExposedPort != null) {
                addFixedExposedPort(fixedExposedPort, PUBSUB_INTERNAL_PORT);
            } else {
                addExposedPort(PUBSUB_INTERNAL_PORT);
            }
        }
    }
}
