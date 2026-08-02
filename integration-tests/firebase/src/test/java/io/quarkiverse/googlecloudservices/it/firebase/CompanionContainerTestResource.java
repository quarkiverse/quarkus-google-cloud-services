package io.quarkiverse.googlecloudservices.it.firebase;

import java.util.Map;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Starts a minimal companion container - unrelated to the application under test, comparable to e.g. a Playwright
 * browser container a real project might start - that reaches the Firebase Realtime Database emulator via the
 * {@code host.testcontainers.internal} ambassador exposed through the
 * {@code quarkus.google.cloud.firebase.database.container-host-override} dev-services property. This verifies
 * that companion-container connectivity actually works end to end, without pulling in a browser-automation
 * dependency (e.g. Playwright) just to prove the ambassador is reachable.
 * <p>
 * A {@link QuarkusTestResourceLifecycleManager} can't fail a test directly, so the observed outcome is recorded as
 * the {@code test.companion.container.status} config property ({@code reachable}, {@code unreachable}, or
 * {@code absent} if the dev-services property itself wasn't published) - the actual assertion happens in whichever
 * test class uses this resource.
 */
public class CompanionContainerTestResource implements QuarkusTestResourceLifecycleManager, DevServicesContext.ContextAware {

    private static final String CONTAINER_HOST_OVERRIDE_PROPERTY = "quarkus.google.cloud.firebase.database.container-host-override";
    private static final String STATUS_PROPERTY = "test.companion.container.status";

    private DevServicesContext context;
    private GenericContainer<?> companion;

    @Override
    public void setIntegrationTestContext(DevServicesContext context) {
        this.context = context;
    }

    @Override
    public Map<String, String> start() {
        var ambassadorUrl = context.devServicesProperties().get(CONTAINER_HOST_OVERRIDE_PROPERTY);
        if (ambassadorUrl == null) {
            // expose-to-companion-containers is disabled for this test - nothing to reach.
            return Map.of(STATUS_PROPERTY, "absent");
        }

        // Kept alive with an overridden entrypoint (curl's own default entrypoint exits immediately), so we can
        // exec curl commands against it afterward.
        companion = new GenericContainer<>(DockerImageName.parse("curlimages/curl:8.11.0"))
                .withCreateContainerCmdModifier(cmd -> cmd.withEntrypoint("sleep").withCmd("300"));
        companion.start();

        try {
            var result = companion.execInContainer("curl", "-s", "-o", "/dev/null", "-w", "%{http_code}",
                    ambassadorUrl + "/.json");
            var httpCode = result.getStdout();
            var reachable = httpCode != null && httpCode.matches("\\d{3}");
            return Map.of(STATUS_PROPERTY, reachable ? "reachable" : "unreachable");
        } catch (Exception e) {
            return Map.of(STATUS_PROPERTY, "unreachable");
        }
    }

    @Override
    public void stop() {
        if (companion != null) {
            companion.stop();
        }
    }
}
