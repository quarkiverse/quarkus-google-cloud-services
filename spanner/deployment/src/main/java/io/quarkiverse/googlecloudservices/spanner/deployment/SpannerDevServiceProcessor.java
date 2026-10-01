package io.quarkiverse.googlecloudservices.spanner.deployment;

import static io.quarkus.devservices.common.ConfigureUtil.configureSharedServiceLabel;
import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;
import org.testcontainers.containers.SpannerEmulatorContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.deployment.IsDevServicesSupportedByLaunchMode;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.devservices.common.ComposeLocator;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.devservices.common.StartableContainer;
import io.quarkus.runtime.LaunchMode;

/**
 * Processor responsible for managing Spanner Services.
 * <p>
 * The processor discovers a running Spanner emulator, or declares an owned Dev Service that Quarkus starts
 * at application startup and reuses across restarts of the application (for example the restart groups of
 * {@code @QuarkusTest}).
 */
@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class })
public class SpannerDevServiceProcessor {

    private static final Logger LOGGER = Logger.getLogger(SpannerDevServiceProcessor.class.getName());

    private static final String EMULATOR_HOST_PROPERTY = "quarkus.google.cloud.spanner.emulator-host";
    private static final int HTTP_PORT = 9020;
    private static final int GRPC_PORT = 9010;

    /**
     * Label to add to the shared Dev Service for Spanner running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-google-cloud-spanner";

    // The emulator host property points to the gRPC endpoint
    private static final ContainerLocator CONTAINER_LOCATOR = locateContainerWithLabels(GRPC_PORT, DEV_SERVICE_LABEL);

    @BuildStep
    public void start(
            DockerStatusBuildItem dockerStatusBuildItem,
            SpannerBuildTimeConfig spannerBuildTimeConfig,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            LaunchModeBuildItem launchMode,
            DevServicesConfig devServicesConfig,
            BuildProducer<DevServicesResultBuildItem> devServicesResult) {
        SpannerDevServiceConfig config = spannerBuildTimeConfig.devservice();

        if (!config.enabled()) {
            // Spanner service explicitly disabled
            LOGGER.debug("Not starting Dev Services for Spanner as it has been disabled in the config");
            return;
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            LOGGER.warn("Not starting devservice because docker is not available");
            return;
        }

        boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                devServicesSharedNetworkBuildItem);

        DevServicesResultBuildItem discovered = discoverRunningService(composeProjectBuildItem, config,
                launchMode.getLaunchMode(), useSharedNetwork);
        if (discovered != null) {
            devServicesResult.produce(discovered);
            return;
        }

        LaunchMode mode = launchMode.getLaunchMode();
        Optional<Duration> timeout = devServicesConfig.timeout();
        String networkId = composeProjectBuildItem.getDefaultNetworkId();

        devServicesResult.produce(DevServicesResultBuildItem.owned()
                .feature(SpannerBuildSteps.FEATURE)
                .serviceName(config.serviceName())
                .serviceConfig(config)
                .startable(() -> {
                    QuarkusSpannerContainer container = new QuarkusSpannerContainer(
                            DockerImageName.parse(config.imageName())
                                    .asCompatibleSubstituteFor(
                                            "gcr.io/google.com/cloudsdktool/cloud-sdk:emulators"),
                            config.httpPort().orElse(null),
                            config.grpcPort().orElse(null),
                            networkId,
                            useSharedNetwork);
                    timeout.ifPresent(container::withStartupTimeout);
                    configureSharedServiceLabel(container, mode, DEV_SERVICE_LABEL, config.serviceName());
                    return new StartableContainer<>(container, QuarkusSpannerContainer::getEmulatorGrpcEndpoint);
                })
                .configProvider(Map.of(EMULATOR_HOST_PROPERTY, StartableContainer::getConnectionInfo))
                .build());
    }

    /**
     * Look for an already running emulator, either a shared one started by another application or one that is part of a
     * Compose project.
     *
     * @return a discovered Dev Service, or null if no emulator is running
     */
    private DevServicesResultBuildItem discoverRunningService(DevServicesComposeProjectBuildItem composeProjectBuildItem,
            SpannerDevServiceConfig config, LaunchMode launchMode, boolean useSharedNetwork) {
        return CONTAINER_LOCATOR.locateContainer(config.serviceName(), config.shared(), launchMode)
                .or(() -> ComposeLocator.locateContainer(composeProjectBuildItem, List.of(config.imageName()),
                        GRPC_PORT, launchMode, useSharedNetwork))
                .map(address -> DevServicesResultBuildItem.discovered()
                        .feature(SpannerBuildSteps.FEATURE)
                        .containerId(address.getId())
                        .config(Map.of(EMULATOR_HOST_PROPERTY, address.getUrl()))
                        .build())
                .orElse(null);
    }

    /**
     * Class for creating and configuring a Spanner emulator container.
     */
    private static class QuarkusSpannerContainer extends SpannerEmulatorContainer {

        private final Integer fixedHttpPort;
        private final Integer fixedGrpcPort;
        private final boolean useSharedNetwork;
        private final String hostName;

        private QuarkusSpannerContainer(DockerImageName dockerImageName, Integer fixedHttpPort, Integer fixedGrpcPort,
                String defaultNetworkId, boolean useSharedNetwork) {
            super(dockerImageName);
            this.fixedHttpPort = fixedHttpPort;
            this.fixedGrpcPort = fixedGrpcPort;
            this.useSharedNetwork = useSharedNetwork;
            this.hostName = ConfigureUtil.configureNetwork(this, defaultNetworkId, useSharedNetwork, "spanner");
        }

        /**
         * Configures the Spanner emulator container.
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
