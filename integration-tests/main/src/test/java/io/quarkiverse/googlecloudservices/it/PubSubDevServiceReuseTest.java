package io.quarkiverse.googlecloudservices.it;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

import com.github.dockerjava.api.model.Container;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Every {@code @QuarkusTest} restart group (here the default one and the one of
 * {@link PubSubDevServiceReuseProfileTest}) must reuse the same Pub/Sub emulator, whatever the order the groups run in,
 * instead of leaving one running emulator behind per group.
 */
@QuarkusTest
public class PubSubDevServiceReuseTest {

    private static final String EMULATOR_IMAGE = "google-cloud-cli:emulators";
    private static final int PUBSUB_EMULATOR_PORT = 8085;

    @Test
    void shouldRunASingleEmulator() {
        assertSingleRunningEmulator();
    }

    static void assertSingleRunningEmulator() {
        // The shared service label is only set in dev mode, so identify the Pub/Sub emulator by its image and port
        List<Container> emulators = DockerClientFactory.lazyClient()
                .listContainersCmd()
                .exec()
                .stream()
                .filter(container -> container.getImage().endsWith(EMULATOR_IMAGE))
                .filter(container -> Arrays.stream(container.getPorts())
                        .anyMatch(port -> port.getPrivatePort() != null && port.getPrivatePort() == PUBSUB_EMULATOR_PORT))
                .toList();

        assertEquals(1, emulators.size(), "Running Pub/Sub emulators: " + emulators.stream().map(Container::getId).toList());
    }
}
