package io.quarkiverse.googlecloudservices.bigtable.deployment;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;
import org.testcontainers.containers.BigtableEmulatorContainer;
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
 * Processor responsible for managing Bigtable Dev Services.
 * <p>
 * The processor starts the Bigtable service in case it's not running.
 */
@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = DevServicesConfig.Enabled.class)
public class BigtableDevServiceProcessor {

    private static final Logger LOGGER = Logger.getLogger(BigtableDevServiceProcessor.class.getName());

    // Running dev service instance
    private static volatile DevServicesResultBuildItem.RunningDevService devService;
    // Configuration for the Bigtable Dev service
    private static volatile BigtableDevServiceConfig config;

    @BuildStep
    public DevServicesResultBuildItem start(
            DockerStatusBuildItem dockerStatusBuildItem,
            BigtableBuildTimeConfig buildTimeConfig,
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
                (launchMode.isTest() ? "(test) " : "") + "Google Cloud Bigtable Dev Services Starting:",
                consoleInstalledBuildItem,
                loggingSetupBuildItem);

        // Try starting the container if conditions are met
        try {
            boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                    devServicesSharedNetworkBuildItem);
            devService = startContainerIfAvailable(dockerStatusBuildItem, buildTimeConfig.devservice(),
                    devServicesConfig.timeout(), composeProjectBuildItem, useSharedNetwork);
        } catch (Throwable t) {
            LOGGER.warn("Unable to start Bigtable dev service", t);
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
     * @param config, Configuration for the Bigtable service
     * @param timeout, Optional timeout for starting the service
     * @param composeProjectBuildItem The compose build item
     * @param useSharedNetwork Start the service on a shared docker network
     * @return Running service item, or null if the service couldn't be started
     */
    private DevServicesResultBuildItem.RunningDevService startContainerIfAvailable(
            DockerStatusBuildItem dockerStatusBuildItem,
            BigtableDevServiceConfig config,
            Optional<Duration> timeout,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            boolean useSharedNetwork) {
        if (!config.enabled()) {
            // Bigtable service explicitly disabled
            LOGGER.debug("Not starting Dev Services for Bigtable as it has been disabled in the config");
            return null;
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            LOGGER.warn("Not starting devservice because docker is not available");
            return null;
        }

        return startContainer(dockerStatusBuildItem, config, timeout, composeProjectBuildItem, useSharedNetwork);
    }

    /**
     * Starts the Bigtable emulator container with provided configuration.
     *
     * @param dockerStatusBuildItem, Docker status
     * @param config, Configuration for the Bigtable service
     * @param timeout, Optional timeout for starting the service
     * @param composeProjectBuildItem The compose build item
     * @param useSharedNetwork Start the service on a shared docker network
     * @return Running service item, or null if the service couldn't be started
     */
    private DevServicesResultBuildItem.RunningDevService startContainer(
            DockerStatusBuildItem dockerStatusBuildItem,
            BigtableDevServiceConfig config,
            Optional<Duration> timeout,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            boolean useSharedNetwork) {
        // Create and configure Bigtable emulator container
        BigtableEmulatorContainer emulatorContainer = new QuarkusBigtableContainer(
                DockerImageName.parse(config.imageName())
                        .asCompatibleSubstituteFor("gcr.io/google.com/cloudsdktool/cloud-sdk:emulators"),
                config.emulatorPort().orElse(null),
                composeProjectBuildItem.getDefaultNetworkId(),
                useSharedNetwork);

        // Set container startup timeout if provided
        timeout.ifPresent(emulatorContainer::withStartupTimeout);
        emulatorContainer.start();

        // Set the config for the started container
        BigtableDevServiceProcessor.config = config;

        // Return running service item with container details
        return new DevServicesResultBuildItem.RunningDevService(BigtableBuildSteps.FEATURE,
                emulatorContainer.getContainerId(),
                emulatorContainer::close, "quarkus.google.cloud.bigtable.emulator-host",
                emulatorContainer.getEmulatorEndpoint());
    }

    /**
     * Stops the running Bigtable emulator container.
     */
    private void stopContainer() {
        if (devService != null && devService.isOwner()) {
            try {
                // Try closing the running dev service
                devService.close();
            } catch (Throwable e) {
                LOGGER.error("Failed to stop Bigtable container", e);
            } finally {
                devService = null;
            }
        }
    }

    /**
     * Class for creating and configuring a Bigtable emulator container.
     */
    private static class QuarkusBigtableContainer extends BigtableEmulatorContainer {

        private final Integer fixedExposedPort;
        private final boolean useSharedNetwork;
        private final String hostName;
        private static final int INTERNAL_PORT = 9000;

        private QuarkusBigtableContainer(DockerImageName dockerImageName, Integer fixedExposedPort,
                String defaultNetworkId, boolean useSharedNetwork) {
            super(dockerImageName);
            this.fixedExposedPort = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;
            this.hostName = ConfigureUtil.configureNetwork(this, defaultNetworkId, useSharedNetwork, "bigtable");
        }

        /**
         * Configures the Bigtable emulator container.
         */
        @Override
        public void configure() {
            super.configure();
            if (useSharedNetwork) {
                return;
            }

            // Expose Bigtable emulatorPort
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
