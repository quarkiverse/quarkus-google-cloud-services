package io.quarkiverse.googlecloudservices.bigtable.deployment;

import static io.quarkus.devservices.common.ConfigureUtil.configureSharedServiceLabel;
import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;
import org.testcontainers.containers.BigtableEmulatorContainer;
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
 * Processor responsible for managing Bigtable Dev Services.
 * <p>
 * The processor discovers a running Bigtable emulator, or declares an owned Dev Service that Quarkus starts
 * at application startup and reuses across restarts of the application (for example the restart groups of
 * {@code @QuarkusTest}).
 */
@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class })
public class BigtableDevServiceProcessor {

    private static final Logger LOGGER = Logger.getLogger(BigtableDevServiceProcessor.class.getName());

    private static final String EMULATOR_HOST_PROPERTY = "quarkus.google.cloud.bigtable.emulator-host";
    private static final int INTERNAL_PORT = 9000;

    /**
     * Label to add to the shared Dev Service for Bigtable running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-google-cloud-bigtable";

    private static final ContainerLocator CONTAINER_LOCATOR = locateContainerWithLabels(INTERNAL_PORT, DEV_SERVICE_LABEL);

    @BuildStep
    public void start(
            DockerStatusBuildItem dockerStatusBuildItem,
            BigtableBuildTimeConfig buildTimeConfig,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            LaunchModeBuildItem launchMode,
            DevServicesConfig devServicesConfig,
            BuildProducer<DevServicesResultBuildItem> devServicesResult) {
        BigtableDevServiceConfig config = buildTimeConfig.devservice();

        if (!config.enabled()) {
            // Bigtable service explicitly disabled
            LOGGER.debug("Not starting Dev Services for Bigtable as it has been disabled in the config");
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
                .feature(BigtableBuildSteps.FEATURE)
                .serviceName(config.serviceName())
                .serviceConfig(config)
                .startable(() -> {
                    QuarkusBigtableContainer container = new QuarkusBigtableContainer(
                            DockerImageName.parse(config.imageName())
                                    .asCompatibleSubstituteFor("gcr.io/google.com/cloudsdktool/cloud-sdk:emulators"),
                            config.emulatorPort().orElse(null),
                            networkId,
                            useSharedNetwork);
                    timeout.ifPresent(container::withStartupTimeout);
                    configureSharedServiceLabel(container, mode, DEV_SERVICE_LABEL, config.serviceName());
                    return new StartableContainer<>(container, QuarkusBigtableContainer::getEmulatorEndpoint);
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
            BigtableDevServiceConfig config, LaunchMode launchMode, boolean useSharedNetwork) {
        return CONTAINER_LOCATOR.locateContainer(config.serviceName(), config.shared(), launchMode)
                .or(() -> ComposeLocator.locateContainer(composeProjectBuildItem, List.of(config.imageName()),
                        INTERNAL_PORT, launchMode, useSharedNetwork))
                .map(address -> DevServicesResultBuildItem.discovered()
                        .feature(BigtableBuildSteps.FEATURE)
                        .containerId(address.getId())
                        .config(Map.of(EMULATOR_HOST_PROPERTY, address.getUrl()))
                        .build())
                .orElse(null);
    }

    /**
     * Class for creating and configuring a Bigtable emulator container.
     */
    private static class QuarkusBigtableContainer extends BigtableEmulatorContainer {

        private final Integer fixedExposedPort;
        private final boolean useSharedNetwork;
        private final String hostName;

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
