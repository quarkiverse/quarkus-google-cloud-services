package io.quarkiverse.googlecloudservices.it.firebase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

/**
 * With the default configuration, {@code expose-to-companion-containers} is enabled, so a companion container
 * started by the test's own code (see {@link CompanionContainerTestResource}) should be able to reach the
 * Firebase emulator via the {@code host.testcontainers.internal} ambassador - regardless of whether shared-network
 * mode is active for this particular test (it isn't here, since this is a plain {@code @QuarkusTest}).
 */
@QuarkusTest
@QuarkusTestResource(CompanionContainerTestResource.class)
class CompanionContainerConnectivityTest {

    @ConfigProperty(name = "test.companion.container.status")
    String companionContainerStatus;

    @Test
    void companionContainerCanReachEmulatorViaAmbassador() {
        assertEquals("reachable", companionContainerStatus);
    }
}
