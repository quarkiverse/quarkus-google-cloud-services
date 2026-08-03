package io.quarkiverse.googlecloudservices.it.firebase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

/**
 * With {@code expose-to-companion-containers=false}, the {@code container-host-override} dev-services property
 * should not be published at all, so {@link CompanionContainerTestResource} never gets an ambassador URL to test.
 */
@QuarkusTest
@TestProfile(NoCompanionContainerExposureProfile.class)
@QuarkusTestResource(CompanionContainerTestResource.class)
class CompanionContainerDisabledTest {

    @ConfigProperty(name = "test.companion.container.status")
    String companionContainerStatus;

    @Test
    void ambassadorPropertyAbsentWhenExposureDisabled() {
        assertEquals("absent", companionContainerStatus);
    }
}
