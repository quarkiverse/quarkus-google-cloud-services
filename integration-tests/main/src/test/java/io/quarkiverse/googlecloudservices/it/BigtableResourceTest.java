package io.quarkiverse.googlecloudservices.it;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class BigtableResourceTest {
    private static final int PORT = 8086;

    private static GenericContainer<?> GCLOUD_CONTAINER;

    @BeforeAll
    public static void startGcloudContainer() {
        GCLOUD_CONTAINER = new GenericContainer<>("marcelcorso/gcloud-bigtable-emulator")
                .withExposedPorts(PORT)
                .waitingFor(new LogMessageWaitStrategy().withRegEx("(?s).*running.*$"));
        List<String> portBindings = new ArrayList<>();
        portBindings.add("8086:8086");
        GCLOUD_CONTAINER.setPortBindings(portBindings);
        GCLOUD_CONTAINER.start();
    }

    @AfterAll
    public static void stopGcloudContainer() {
        if (GCLOUD_CONTAINER != null) {
            GCLOUD_CONTAINER.stop();
        }
    }

    @Test
    @SetEnvironmentVariable(key = "BIGTABLE_EMULATOR_HOST", value = "localhost:8086")
    public void testBigtable() {
        given()
                .when().get("/bigtable")
                .then()
                .statusCode(200);
    }
}
