package io.quarkiverse.googlecloudservices.spanner.deployment;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;
import org.testcontainers.containers.SpannerEmulatorContainer;
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
 * Processor responsible for managing Spanner Services.
 * <p>
 * The processor starts the Spanner service in case it's not running.
 */
@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = DevServicesConfig.Enabled.class)
public class SpannerDevServiceProcessor {

    private static final Logger LOGGER = Logger.getLogger(SpannerDevServiceProcessor.class.getName());

    // Running dev service instance
    private static volatile DevServicesResultBuildItem.RunningDevService devService;
    // Configuration for the Pub/Sub Dev service
    private static volatile SpannerDevServiceConfig config;

    @BuildStep
    public DevServicesResultBuildItem start(
            DockerStatusBuildItem dockerStatusBuildItem,
            SpannerBuildTimeConfig spannerBuildTimeConfig,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LaunchModeBuildItem launchMode,
            LoggingSetupBuildItem loggingSetupBuildItem,
            DevServicesConfig devServicesConfig) {
        // If dev service is running and config has changed, stop the service
        if (devService != null && !spannerBuildTimeConfig.devservice().equals(config)) {
            stopContainer();
        } else if (devService != null) {
            return devService.toBuildItem();
        }

        // Set up log compressor for startup logs
        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Google Cloud Spanner Dev Services Starting:",
                consoleInstalledBuildItem,
                loggingSetupBuildItem);

        // Try starting the container if conditions are met
        try {
            boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                    devServicesSharedNetworkBuildItem);
            devService = startContainerIfAvailable(dockerStatusBuildItem, spannerBuildTimeConfig.devservice(),
                    devServicesConfig.timeout(), composeProjectBuildItem, useSharedNetwork);
        } catch (Throwable t) {
            LOGGER.warn("Unable to start Spanner dev service", t);
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
     * @param config, Configuration for the Spanner service
     * @param timeout, Optional timeout for starting the service
     * @param composeProjectBuildItem The compose build item
     * @param useSharedNetwork Start the service on a shared docker network
     * @return Running service item, or null if the service couldn't be started
     */
    private DevServicesResultBuildItem.RunningDevService startContainerIfAvailable(
            DockerStatusBuildItem dockerStatusBuildItem,
            SpannerDevServiceConfig config,
            Optional<Duration> timeout,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            boolean useSharedNetwork) {
        if (!config.enabled()) {
            // Spanner service explicitly disabled
            LOGGER.debug("Not starting Dev Services for Spanner as it has been disabled in the config");
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
     * @param config, Configuration for the Spanner service
     * @param timeout, Optional timeout for starting the service
     * @param composeProjectBuildItem The compose build item
     * @param useSharedNetwork Start the service on a shared docker network
     * @return Running service item, or null if the service couldn't be started
     */
    private DevServicesResultBuildItem.RunningDevService startContainer(
            DockerStatusBuildItem dockerStatusBuildItem,
            SpannerDevServiceConfig config,
            Optional<Duration> timeout,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            boolean useSharedNetwork) {
        // Create and configure Pub/Sub emulator container
        QuarkusSpannerContainer emulatorContainer = new QuarkusSpannerContainer(
                DockerImageName.parse(config.imageName())
                        .asCompatibleSubstituteFor("gcr.io/google.com/cloudsdktool/cloud-sdk:emulators:emulators"),
                config.httpPort().orElse(null),
                config.grpcPort().orElse(null),
                composeProjectBuildItem.getDefaultNetworkId(),
                useSharedNetwork);

        // Set container startup timeout if provided
        timeout.ifPresent(emulatorContainer::withStartupTimeout);
        emulatorContainer.start();

        // Set the config for the started container
        SpannerDevServiceProcessor.config = config;

        // Return running service item with container details
        return new DevServicesResultBuildItem.RunningDevService(SpannerBuildSteps.FEATURE,
                emulatorContainer.getContainerId(),
                emulatorContainer::close, "quarkus.google.cloud.spanner.emulator-host",
                emulatorContainer.getEmulatorGrpcEndpoint());
    }

    /**
     * Stops the running Spanner emulator container.
     */
    private void stopContainer() {
        if (devService != null && devService.isOwner()) {
            try {
                // Try closing the running dev service
                devService.close();
            } catch (Throwable e) {
                LOGGER.error("Failed to stop spanner container", e);
            } finally {
                devService = null;
            }
        }
    }

    /**
     * Class for creating and configuring a Spanner emulator container.
     */
    private static class QuarkusSpannerContainer extends SpannerEmulatorContainer {

        private final Integer fixedHttpPort;
        private final Integer fixedGrpcPort;
        private final boolean useSharedNetwork;
        private final String hostName;
        private static final int HTTP_PORT = 9020;
        private static final int GRPC_PORT = 9010;

        private QuarkusSpannerContainer(DockerImageName dockerImageName, Integer fixedHttpPort, Integer fixedGrpcPort,
                String defaultNetworkId, boolean useSharedNetwork) {
            super(dockerImageName);
            this.fixedHttpPort = fixedHttpPort;
            this.fixedGrpcPort = fixedGrpcPort;
            this.useSharedNetwork = useSharedNetwork;
            this.hostName = ConfigureUtil.configureNetwork(this, defaultNetworkId, useSharedNetwork, "spanner");
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

            // Expose HTTP emulatorPort
            if (fixedHttpPort != null) {
                addFixedExposedPort(fixedHttpPort, HTTP_PORT);
            } else {
                addExposedPort(HTTP_PORT);
            }

            // Expose GRPC emulatorPort
            if (fixedGrpcPort != null) {
                addFixedExposedPort(fixedGrpcPort, GRPC_PORT);
            } else {
                addExposedPort(GRPC_PORT);
            }
        }

        @Override
        public String getEmulatorGrpcEndpoint() {
            if (useSharedNetwork) {
                return hostName + ":" + GRPC_PORT;
            } else {
                return super.getEmulatorGrpcEndpoint();
            }
        }

        @Override
        public String getEmulatorHttpEndpoint() {
            if (useSharedNetwork) {
                return hostName + ":" + HTTP_PORT;
            } else {
                return super.getEmulatorGrpcEndpoint();
            }
        }
    }

}
