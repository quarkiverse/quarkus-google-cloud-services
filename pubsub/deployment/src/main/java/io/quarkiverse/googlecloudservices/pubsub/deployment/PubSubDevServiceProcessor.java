package io.quarkiverse.googlecloudservices.pubsub.deployment;

import static io.quarkus.devservices.common.ConfigureUtil.configureSharedServiceLabel;
import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;
import org.testcontainers.gcloud.PubSubEmulatorContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkiverse.googlecloudservices.pubsub.push.PubSubPushBuildTimeConfig;
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
 * Processor responsible for managing Pub/Sub Dev Services.
 * <p>
 * The processor discovers a running Pub/Sub emulator, or declares an owned Dev Service that Quarkus starts
 * at application startup and reuses across restarts of the application (for example the restart groups of
 * {@code @QuarkusTest}).
 */
@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class })
public class PubSubDevServiceProcessor {

    private static final Logger LOGGER = Logger.getLogger(PubSubDevServiceProcessor.class.getName());

    private static final String EMULATOR_HOST_PROPERTY = "quarkus.google.cloud.pubsub.emulator-host";
    private static final int INTERNAL_PORT = 8085;

    /**
     * Label to add to the shared Dev Service for Pub/Sub running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-google-cloud-pubsub";

    private static final ContainerLocator CONTAINER_LOCATOR = locateContainerWithLabels(INTERNAL_PORT, DEV_SERVICE_LABEL);

    @BuildStep
    public void start(
            DockerStatusBuildItem dockerStatusBuildItem,
            PubSubDevServiceConfig devServiceConfig,
            PubSubPushBuildTimeConfig pushConfig,
            FirebaseDevServiceConfig firebaseConfig,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            LaunchModeBuildItem launchMode,
            DevServicesConfig devServicesConfig,
            BuildProducer<DevServicesResultBuildItem> devServicesResult) {
        if (!devServiceConfig.enabled()) {
            // PubSub service explicitly disabled
            LOGGER.debug("Not starting Dev Services for PubSub as it has been disabled in the config");
            return;
        }

        if (firebaseConfig.preferFirebaseDevServices().orElse(false)) {
            // Firebase DevServices are included, use them instead
            LOGGER.debug("Not starting Dev Services for PubSub as the Firebase DevServices are preferred");
            return;
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            LOGGER.warn("Not starting devservice because docker is not available");
            return;
        }

        boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                devServicesSharedNetworkBuildItem);

        // A discovered emulator cannot be given the host-gateway mapping that push subscriptions need,
        // so discovery is only used when push is disabled.
        DevServicesResultBuildItem discovered = pushConfig.enabled() ? null
                : discoverRunningService(composeProjectBuildItem, devServiceConfig, launchMode.getLaunchMode(),
                        useSharedNetwork);
        if (discovered != null) {
            devServicesResult.produce(discovered);
            return;
        }

        LaunchMode mode = launchMode.getLaunchMode();
        boolean push = pushConfig.enabled();
        Optional<Duration> timeout = devServicesConfig.timeout();
        String networkId = composeProjectBuildItem.getDefaultNetworkId();

        devServicesResult.produce(DevServicesResultBuildItem.owned()
                .feature(PubSubBuildSteps.FEATURE)
                .serviceName(devServiceConfig.serviceName())
                // Quarkus compares this value between restarts of the application, each one having its own classloader,
                // so it must only contain JDK types or config mappings.
                .serviceConfig(Map.of(
                        "imageName", devServiceConfig.imageName(),
                        "emulatorPort", devServiceConfig.emulatorPort(),
                        "shared", devServiceConfig.shared(),
                        "serviceName", devServiceConfig.serviceName(),
                        "push", push,
                        "useSharedNetwork", useSharedNetwork))
                .startable(() -> {
                    QuarkusPubSubContainer container = new QuarkusPubSubContainer(
                            DockerImageName.parse(devServiceConfig.imageName())
                                    .asCompatibleSubstituteFor("gcr.io/google.com/cloudsdktool/cloud-sdk:emulators"),
                            devServiceConfig.emulatorPort().orElse(null),
                            networkId,
                            useSharedNetwork,
                            push);
                    timeout.ifPresent(container::withStartupTimeout);
                    configureSharedServiceLabel(container, mode, DEV_SERVICE_LABEL, devServiceConfig.serviceName());
                    return new StartableContainer<>(container, QuarkusPubSubContainer::getEmulatorEndpoint);
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
            PubSubDevServiceConfig config, LaunchMode launchMode, boolean useSharedNetwork) {
        return CONTAINER_LOCATOR.locateContainer(config.serviceName(), config.shared(), launchMode)
                .or(() -> ComposeLocator.locateContainer(composeProjectBuildItem, List.of(config.imageName()),
                        INTERNAL_PORT, launchMode, useSharedNetwork))
                .map(address -> DevServicesResultBuildItem.discovered()
                        .feature(PubSubBuildSteps.FEATURE)
                        .containerId(address.getId())
                        .config(Map.of(EMULATOR_HOST_PROPERTY, address.getUrl()))
                        .build())
                .orElse(null);
    }

    /**
     * Class for creating and configuring a PubSub emulator container.
     */
    private static class QuarkusPubSubContainer extends PubSubEmulatorContainer {

        private final Integer fixedExposedPort;
        private final boolean useSharedNetwork;
        private final boolean usePush;
        private final String hostName;

        private QuarkusPubSubContainer(DockerImageName dockerImageName, Integer fixedExposedPort,
                String defaultNetworkId, boolean useSharedNetwork, boolean usePush) {
            super(dockerImageName);
            this.fixedExposedPort = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;
            this.hostName = ConfigureUtil.configureNetwork(this, defaultNetworkId, useSharedNetwork, "pubsub");
            this.usePush = usePush;
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

            if (usePush) {
                withExtraHost("host.docker.internal", "host-gateway");
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
