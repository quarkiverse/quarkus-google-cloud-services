package io.quarkiverse.googlecloudservices.firebase.deployment;

import static io.quarkus.devservices.common.ConfigureUtil.configureSharedServiceLabel;
import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.testcontainers.Testcontainers;

import io.quarkiverse.googlecloudservices.firebase.deployment.testcontainers.FirebaseEmulatorContainer;
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
import io.quarkus.devservices.common.ContainerAddress;
import io.quarkus.devservices.common.StartableContainer;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.ExternalPageBuilder;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.runtime.LaunchMode;

/**
 * Processor responsible for managing Firebase Dev Services.
 * <p>
 * The processor discovers a running Firebase emulator, or declares an owned Dev Service that Quarkus starts
 * at application startup and reuses across restarts of the application (for example the restart groups of
 * {@code @QuarkusTest}).
 */
@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class })
public class FirebaseDevServiceProcessor {

    private static final Logger LOGGER = Logger.getLogger(FirebaseDevServiceProcessor.class.getName());

    /**
     * Label to add to the shared Dev Service for Firebase running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-google-cloud-firebase";

    private static final Map<FirebaseEmulatorContainer.Emulator, String> CONFIG_PROPERTIES = Map.of(
            FirebaseEmulatorContainer.Emulator.AUTHENTICATION, "quarkus.google.cloud.firebase.auth.emulator-host",
            FirebaseEmulatorContainer.Emulator.EMULATOR_SUITE_UI, "quarkus.google.cloud.firebase.emulator-host",
            FirebaseEmulatorContainer.Emulator.EMULATOR_HUB, "quarkus.google.cloud.firebase.hub-host",
            FirebaseEmulatorContainer.Emulator.FIREBASE_HOSTING, "quarkus.google.cloud.firebase.hosting.emulator-host",
            FirebaseEmulatorContainer.Emulator.CLOUD_FUNCTIONS, "quarkus.google.cloud.functions.emulator-host",
            FirebaseEmulatorContainer.Emulator.EVENT_ARC, "quarkus.google.cloud.eventarc.emulator-host",
            FirebaseEmulatorContainer.Emulator.REALTIME_DATABASE, "quarkus.google.cloud.firebase.database.host-override",
            FirebaseEmulatorContainer.Emulator.CLOUD_FIRESTORE, "quarkus.google.cloud.firestore.host-override",
            FirebaseEmulatorContainer.Emulator.CLOUD_STORAGE, "quarkus.google.cloud.storage.host-override",
            FirebaseEmulatorContainer.Emulator.PUB_SUB, "quarkus.google.cloud.pubsub.emulator-host");

    // Additional config properties exposing the emulator endpoints as reachable from any other container
    // started by Testcontainers in this JVM (e.g. the Playwright browser container, or a containerized
    // app-under-test), via Testcontainers.exposeHostPorts() + the "host.testcontainers.internal" ambassador
    // hostname. A container started outside Testcontainers (e.g. via docker-compose) won't have that ambassador
    // wired up and must instead use the shared-network alias (see FirebaseEmulatorContainer#emulatorUrl).
    private static final Map<FirebaseEmulatorContainer.Emulator, String> CONTAINER_CONFIG_PROPERTIES = Map.of(
            FirebaseEmulatorContainer.Emulator.AUTHENTICATION, "quarkus.google.cloud.firebase.auth.container-emulator-host",
            FirebaseEmulatorContainer.Emulator.EMULATOR_SUITE_UI, "quarkus.google.cloud.firebase.container-emulator-host",
            FirebaseEmulatorContainer.Emulator.EMULATOR_HUB, "quarkus.google.cloud.firebase.container-hub-host",
            FirebaseEmulatorContainer.Emulator.FIREBASE_HOSTING,
            "quarkus.google.cloud.firebase.hosting.container-emulator-host",
            FirebaseEmulatorContainer.Emulator.CLOUD_FUNCTIONS, "quarkus.google.cloud.functions.container-emulator-host",
            FirebaseEmulatorContainer.Emulator.EVENT_ARC, "quarkus.google.cloud.eventarc.container-emulator-host",
            FirebaseEmulatorContainer.Emulator.REALTIME_DATABASE,
            "quarkus.google.cloud.firebase.database.container-host-override",
            FirebaseEmulatorContainer.Emulator.CLOUD_FIRESTORE, "quarkus.google.cloud.firestore.container-host-override",
            FirebaseEmulatorContainer.Emulator.CLOUD_STORAGE, "quarkus.google.cloud.storage.container-host-override",
            FirebaseEmulatorContainer.Emulator.PUB_SUB, "quarkus.google.cloud.pubsub.container-emulator-host");

    private static final String HOST_TESTCONTAINERS_INTERNAL = "host.testcontainers.internal";

    @BuildStep
    public void start(
            DockerStatusBuildItem dockerStatusBuildItem,
            FirebaseDevServiceProjectConfig projectConfig,
            FirebaseDevServiceConfig firebaseBuildTimeConfig,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            LaunchModeBuildItem launchMode,
            BuildProducer<CardPageBuildItem> cardProducer,
            DevServicesConfig devServicesConfig,
            BuildProducer<DevServicesResultBuildItem> devServicesResult) {
        if (!firebaseBuildTimeConfig.firebase().preferFirebaseDevServices()) {
            // Firebase service explicitly disabled
            LOGGER.info("Not starting Dev Services for Firebase as it has been disabled in the config.");
            return;
        }

        if (!isEnabled(firebaseBuildTimeConfig)) {
            // Firebase service implicitly disabled, no emulators enabled.
            LOGGER.info("Not starting Dev Services for Firebase as no emulators are enabled.");
            return;
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            LOGGER.info("Not starting DevService because docker is not available");
            return;
        }

        boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                devServicesSharedNetworkBuildItem);

        FirebaseEmulatorContainer.EmulatorConfig emulatorContainerConfig;
        try {
            emulatorContainerConfig = new FirebaseEmulatorConfigBuilder(
                    projectConfig,
                    firebaseBuildTimeConfig,
                    useSharedNetwork).buildConfig();
        } catch (Throwable t) {
            LOGGER.warn("Unable to configure Firebase dev service", t);
            return;
        }

        // The emulators are known at build time, either from the configuration or from the firebase.json file.
        Set<FirebaseEmulatorContainer.Emulator> emulators = appEmulators(emulatorContainerConfig);

        if (emulators.isEmpty()) {
            LOGGER.info("Not starting Dev Services for Firebase as no emulators are configured.");
            return;
        }

        createDevServiceCard(emulators, emulatorContainerConfig, launchMode, cardProducer);

        LaunchMode mode = launchMode.getLaunchMode();
        var emulatorConfig = firebaseBuildTimeConfig.firebase().emulator();

        DevServicesResultBuildItem discovered = discoverRunningService(emulatorContainerConfig, composeProjectBuildItem,
                emulatorConfig, mode, useSharedNetwork);
        if (discovered != null) {
            LOGGER.debugv("Using discovered Firebase emulator in container {0}", discovered.getContainerId());
            devServicesResult.produce(discovered);
            return;
        } else {
            LOGGER.debug("No running Firebase emulator found, starting a new one");
        }

        Optional<Duration> timeout = devServicesConfig.timeout();
        String networkId = composeProjectBuildItem.getDefaultNetworkId();
        boolean exposeToCompanionContainers = emulatorConfig.exposeToCompanionContainers();
        FirebaseEmulatorContainer.Emulator primaryEmulator = emulators.iterator().next();
        var configProviders = configProviders(emulators, useSharedNetwork, exposeToCompanionContainers);

        devServicesResult.produce(DevServicesResultBuildItem.owned()
                .feature(FirebaseBuildSteps.FEATURE)
                .serviceName(emulatorConfig.serviceName())
                .serviceConfig(serviceConfig(emulatorContainerConfig, emulatorConfig))
                .startable(() -> {
                    // Create and configure Firebase emulator container
                    var emulatorContainer = new FirebaseEmulatorConfigBuilder(projectConfig, firebaseBuildTimeConfig,
                            useSharedNetwork).build();
                    String hostName = ConfigureUtil.configureNetwork(emulatorContainer, networkId, useSharedNetwork,
                            "firebase");

                    // Set container startup timeout if provided
                    timeout.ifPresent(emulatorContainer::withStartupTimeout);
                    emulatorContainer.setupSharedNetworkHost(hostName);
                    configureSharedServiceLabel(emulatorContainer, mode, DEV_SERVICE_LABEL,
                            emulatorConfig.serviceName());
                    return new StartableContainer<>(emulatorContainer, c -> c.hostEmulatorUrl(primaryEmulator));
                })
                .postStartHook(startable -> postStart(startable, configProviders, exposeToCompanionContainers))
                .configProvider(configProviders)
                .build());
    }

    private void createDevServiceCard(Set<FirebaseEmulatorContainer.Emulator> emulators,
            FirebaseEmulatorContainer.EmulatorConfig emulatorContainerConfig,
            LaunchModeBuildItem launchMode,
            BuildProducer<CardPageBuildItem> cardProducer) {
        if (launchMode.isNotLocalDevModeType()) {
            return;
        }

        var cardBuildItem = new CardPageBuildItem();
        cardBuildItem.addBuildTimeData("emulators", emulators
                .stream()
                .map(emulator -> new EmulatorRow(emulator, CONFIG_PROPERTIES.get(emulator)))
                .toList());

        cardBuildItem.addPage(Page.tableDataPageBuilder("Running emulators")
                .showColumn("name")
                .showColumn("configProperty")
                .icon("font-awesome-solid:plug")
                .staticLabel("" + emulators.size())
                .buildTimeDataKey("emulators"));

        // The UI URL is only known once the emulator runs (or has been discovered), so it is resolved at runtime
        // from the config of the Dev Service.
        if (emulators.contains(FirebaseEmulatorContainer.Emulator.EMULATOR_SUITE_UI)) {
            cardBuildItem.addPage(Page.externalPageBuilder("Firebase UI")
                    .dynamicUrlJsonRPCMethodName("devui-dev-services:devServicesConfig",
                            Map.of("name", FirebaseBuildSteps.FEATURE,
                                    "configKey",
                                    CONFIG_PROPERTIES.get(FirebaseEmulatorContainer.Emulator.EMULATOR_SUITE_UI)))
                    .icon("font-awesome-solid:gauge-high")
                    .staticLabel(Optional.ofNullable(emulatorContainerConfig.firebaseVersion()).orElse("auto-detected"))
                    .mimeType(ExternalPageBuilder.MIME_TYPE_HTML));
        }

        cardProducer.produce(cardBuildItem);
    }

    public static class EmulatorRow {
        private final FirebaseEmulatorContainer.Emulator name;
        private final String configProperty;

        public EmulatorRow(FirebaseEmulatorContainer.Emulator name, String configProperty) {
            this.name = name;
            this.configProperty = configProperty;
        }

        public FirebaseEmulatorContainer.Emulator getName() {
            return name;
        }

        public String getConfigProperty() {
            return configProperty;
        }
    }

    private boolean isEnabled(FirebaseDevServiceConfig config) {
        return FirebaseEmulatorConfigBuilder.devServices(config)
                .values()
                .stream()
                .map(FirebaseDevServiceConfig.GenericDevService::enabled)
                .reduce(Boolean.FALSE, Boolean::logicalOr);
    }

    /**
     * Look for an already running emulator, either a shared one started by another application or one that is part of a
     * Compose project. As Firebase runs several emulators in one container, each
     * emulator is located on its own port. The container found by label is addressed on the Docker host and the
     * published port, a container of a Compose project as the {@link ComposeLocator} does (so by the service name and
     * container port when using a shared network).
     *
     * @return a discovered Dev Service, or null if not every emulator is running
     */
    private DevServicesResultBuildItem discoverRunningService(FirebaseEmulatorContainer.EmulatorConfig emulatorConfig,
            DevServicesComposeProjectBuildItem composeProject, FirebaseDevServiceConfig.Firebase.Emulator config,
            LaunchMode launchMode, boolean useSharedNetwork) {

        Set<FirebaseEmulatorContainer.Emulator> emulators = appEmulators(emulatorConfig);

        Set<EmulatorContainerAddress> addresses = emulators.stream()
                .map(emulator -> {
                    int port = emulatorConfig.emulatorPort(emulator);
                    return locateContainerWithLabels(port, DEV_SERVICE_LABEL)
                            .locateContainer(config.serviceName(), config.shared(), launchMode)
                            .or(() -> ComposeLocator.locateContainer(composeProject,
                                    List.of(FirebaseEmulatorContainer.IMAGE_NAME),
                                    port, launchMode, useSharedNetwork))
                            .map(containerAddress -> new EmulatorContainerAddress(emulator, containerAddress));
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());

        if (addresses.size() != emulators.size()) {
            return null;
        }

        Map<String, String> properties = new HashMap<>();
        Set<Integer> hostPorts = new HashSet<>();

        addresses.forEach(emulatorContainerAddress -> {
            ContainerAddress address = emulatorContainerAddress.containerAddress();
            FirebaseEmulatorContainer.Emulator emulator = emulatorContainerAddress.emulator();

            properties.put(CONFIG_PROPERTIES.get(emulator),
                    FirebaseEmulatorContainer.withHttpPrefixIfNeeded(emulator, address.getUrl()));

            // The port is only published to the Docker host if it was found by label or the Compose project is not
            // addressed by its shared network
            boolean publishedToHost = address.getRunningContainer() == null || !useSharedNetwork;
            if (config.exposeToCompanionContainers() && CONTAINER_CONFIG_PROPERTIES.containsKey(emulator)
                    && publishedToHost) {
                properties.put(CONTAINER_CONFIG_PROPERTIES.get(emulator), containerEmulatorUrl(emulator, address.getPort()));
                hostPorts.add(address.getPort());
            }
        });

        if (useSharedNetwork) {
            properties.putAll(emulatorCredentialsConfig(emulators));
        }

        if (LOGGER.isInfoEnabled()) {
            properties
                    .forEach((key, value) -> LOGGER.info("Google Cloud emulator config property " + key + " set to " + value));
        }

        // A discovered Dev Service has no post start hook, so the host ports are exposed to the containers started
        // by Testcontainers in this JVM right here, as that is needed per JVM.
        if (!hostPorts.isEmpty()) {
            LOGGER.infov("Exposing the discovered Firebase emulator ports {0} to companion containers", hostPorts);
            Testcontainers.exposeHostPorts(hostPorts.stream().mapToInt(Integer::intValue).toArray());
        }

        return DevServicesResultBuildItem.discovered()
                .feature(FirebaseBuildSteps.FEATURE)
                .containerId(addresses.iterator().next().containerAddress().getId())
                .config(properties)
                .build();
    }

    private record EmulatorContainerAddress(FirebaseEmulatorContainer.Emulator emulator,
            ContainerAddress containerAddress) {
    }

    /**
     * The configured emulators for which a config property is exposed to the application.
     */
    private static Set<FirebaseEmulatorContainer.Emulator> appEmulators(
            FirebaseEmulatorContainer.EmulatorConfig emulatorConfig) {
        return emulatorConfig
                .emulators()
                .stream()
                .filter(CONFIG_PROPERTIES::containsKey)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(FirebaseEmulatorContainer.Emulator.class)));
    }

    /**
     * The configuration Quarkus compares between restarts of the application to decide whether the running container
     * can be reused. It is based on the resolved emulator configuration instead of the config mapping, as the
     * same configuration can resolve to a different container, for example when a firebase.json or hosting path
     * differs between branches. It only contains JDK types, as every restart has its own classloader.
     */
    private Map<String, String> serviceConfig(FirebaseEmulatorContainer.EmulatorConfig emulatorContainerConfig,
            FirebaseDevServiceConfig.Firebase.Emulator emulatorConfig) {
        var serviceConfig = new HashMap<>(emulatorContainerConfig.reuseFingerprint());
        serviceConfig.put("exposeToCompanionContainers", String.valueOf(emulatorConfig.exposeToCompanionContainers()));
        return serviceConfig;
    }

    /**
     * The config properties to set once the container is started, as functions of the started container.
     */
    private Map<String, Function<StartableContainer<FirebaseEmulatorContainer>, String>> configProviders(
            Set<FirebaseEmulatorContainer.Emulator> emulators, boolean useSharedNetwork,
            boolean exposeToCompanionContainers) {
        Map<String, Function<StartableContainer<FirebaseEmulatorContainer>, String>> providers = new HashMap<>();

        // App-facing properties: the Docker host by default (reachable from the host-JVM running dev mode or a
        // plain unit test), switched to the shared-network alias when shared-network mode is active.
        for (var emulator : emulators) {
            providers.put(CONFIG_PROPERTIES.get(emulator), useSharedNetwork
                    ? s -> s.getContainer().containerEmulatorUrl(emulator)
                    : s -> s.getContainer().hostEmulatorUrl(emulator));
        }

        // On a shared network the host is the container alias, so the automatic emulator-credentials detection
        // doesn't kick in (see emulatorCredentialsConfig). This is not needed in the other modes, as the host is
        // then localhost.
        if (useSharedNetwork) {
            emulatorCredentialsConfig(emulators).forEach((key, value) -> providers.put(key, s -> value));
        }

        // Expose the emulator's host-mapped ports to any container started by Testcontainers in this JVM (e.g. a
        // Playwright browser container), reachable via the "host.testcontainers.internal" ambassador. This works
        // regardless of shared-network mode - Testcontainers.exposeHostPorts() is independent of it - so it's
        // available whenever the user hasn't opted out, not just when shared-network happens to be on for some
        // unrelated reason.
        if (exposeToCompanionContainers) {
            for (var emulator : emulators) {
                if (CONTAINER_CONFIG_PROPERTIES.containsKey(emulator)) {
                    providers.put(CONTAINER_CONFIG_PROPERTIES.get(emulator),
                            s -> containerEmulatorUrl(emulator, s.getContainer().hostMappedPort(emulator)));
                }
            }
        }

        return providers;
    }

    /**
     * The automatic emulator-credentials detection (e.g. FirestoreProducer#automaticEmulatorCredentials) only
     * triggers when the emulator host contains "localhost". On a shared network the host is the container alias
     * instead, so the detection doesn't kick in and it is forced explicitly.
     */
    private static Map<String, String> emulatorCredentialsConfig(Set<FirebaseEmulatorContainer.Emulator> emulators) {
        Map<String, String> config = new HashMap<>();
        if (emulators.contains(FirebaseEmulatorContainer.Emulator.PUB_SUB)) {
            config.put("quarkus.google.cloud.pubsub.use-emulator-credentials", "true");
        }
        if (emulators.contains(FirebaseEmulatorContainer.Emulator.CLOUD_FIRESTORE)) {
            config.put("quarkus.google.cloud.firestore.use-emulator-credentials", "true");
        }
        return config;
    }

    private void postStart(StartableContainer<FirebaseEmulatorContainer> startable,
            Map<String, Function<StartableContainer<FirebaseEmulatorContainer>, String>> configProviders,
            boolean exposeToCompanionContainers) {
        FirebaseEmulatorContainer emulatorContainer = startable.getContainer();
        if (LOGGER.isInfoEnabled()) {
            var runningPorts = emulatorContainer.hostEmulatorUrls();
            runningPorts.forEach((e, p) -> LOGGER.info("Google Cloud Emulator " + e + " reachable on " + p));

            configProviders.forEach((key, value) -> LOGGER
                    .info("Google Cloud emulator config property " + key + " set to " + value.apply(startable)));
        }

        if (exposeToCompanionContainers) {
            emulatorContainer.hostEmulatorUrls().keySet()
                    .forEach(emulator -> Testcontainers.exposeHostPorts(emulatorContainer.hostMappedPort(emulator)));
        }
    }

    /**
     * Build the URL on which an emulator is reachable from another Testcontainers-managed container, via the
     * {@code host.testcontainers.internal} ambassador (see {@link Testcontainers#exposeHostPorts(int...)}).
     */
    private static String containerEmulatorUrl(FirebaseEmulatorContainer.Emulator emulator, int hostPort) {
        return FirebaseEmulatorContainer.withHttpPrefixIfNeeded(emulator,
                HOST_TESTCONTAINERS_INTERNAL + ":" + hostPort);
    }

}
