package io.quarkiverse.googlecloudservices.firestore.deployment;

import static io.quarkus.devservices.common.ConfigureUtil.configureSharedServiceLabel;
import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;
import org.testcontainers.containers.FirestoreEmulatorContainer;
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
 * Processor responsible for managing Firestore Dev Services.
 * <p>
 * The processor discovers a running Firestore emulator, or declares an owned Dev Service that Quarkus starts
 * at application startup and reuses across restarts of the application (for example the restart groups of
 * {@code @QuarkusTest}).
 */
@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class })
public class FirestoreDevServiceProcessor {

    private static final Logger LOGGER = Logger.getLogger(FirestoreDevServiceProcessor.class.getName());

    private static final String EMULATOR_HOST_PROPERTY = "quarkus.google.cloud.firestore.host-override";
    private static final int INTERNAL_PORT = 8080;

    /**
     * Label to add to the shared Dev Service for Firestore running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-google-cloud-firestore";

    private static final ContainerLocator CONTAINER_LOCATOR = locateContainerWithLabels(INTERNAL_PORT, DEV_SERVICE_LABEL);

    @BuildStep
    public void start(
            DockerStatusBuildItem dockerStatusBuildItem,
            FirestoreBuildTimeConfig buildTimeConfig,
            FirebaseDevServiceConfig firebaseConfig,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            LaunchModeBuildItem launchMode,
            DevServicesConfig devServicesConfig,
            BuildProducer<DevServicesResultBuildItem> devServicesResult) {
        FirestoreDevServiceConfig config = buildTimeConfig.devservice();

        if (!config.enabled()) {
            // Firestore service explicitly disabled
            LOGGER.debug("Not starting Dev Services for Firestore as it has been disabled in the config");
            return;
        }

        if (firebaseConfig.preferFirebaseDevServices().orElse(false)) {
            // Firebase DevServices are included, use them instead
            LOGGER.debug("Not starting Dev Services for Firestore as the Firebase DevServices are preferred");
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
                .feature(FirestoreBuildSteps.FEATURE)
                .serviceName(config.serviceName())
                .serviceConfig(config)
                .startable(() -> {
                    QuarkusFirestoreContainer container = new QuarkusFirestoreContainer(
                            DockerImageName.parse(config.imageName())
                                    .asCompatibleSubstituteFor("gcr.io/google.com/cloudsdktool/cloud-sdk:emulators"),
                            config.emulatorPort().orElse(null),
                            networkId,
                            useSharedNetwork);
                    timeout.ifPresent(container::withStartupTimeout);
                    configureSharedServiceLabel(container, mode, DEV_SERVICE_LABEL, config.serviceName());
                    return new StartableContainer<>(container, QuarkusFirestoreContainer::getEmulatorEndpoint);
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
            FirestoreDevServiceConfig config, LaunchMode launchMode, boolean useSharedNetwork) {
        return CONTAINER_LOCATOR.locateContainer(config.serviceName(), config.shared(), launchMode)
                .or(() -> ComposeLocator.locateContainer(composeProjectBuildItem, List.of(config.imageName()),
                        INTERNAL_PORT, launchMode, useSharedNetwork))
                .map(address -> DevServicesResultBuildItem.discovered()
                        .feature(FirestoreBuildSteps.FEATURE)
                        .containerId(address.getId())
                        .config(Map.of(EMULATOR_HOST_PROPERTY, address.getUrl()))
                        .build())
                .orElse(null);
    }

    /**
     * Class for creating and configuring a Firestore emulator container.
     */
    private static class QuarkusFirestoreContainer extends FirestoreEmulatorContainer {

        private final Integer fixedExposedPort;
        private final boolean useSharedNetwork;
        private final String hostName;

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
