package io.quarkiverse.googlecloudservices.it;

import static io.restassured.RestAssured.given;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class FirestoreResourceTest {
    private static final int PORT = 8080;
    private static final String PROJECT_ID = "my-project-id";

    private static GenericContainer<?> GCLOUD_CONTAINER;

    @BeforeAll
    public static void startGcloudContainer() throws IOException, InterruptedException {
        GCLOUD_CONTAINER = new GenericContainer<>("mtlynch/firestore-emulator")
                .withExposedPorts(PORT)
                .withEnv("FIRESTORE_PROJECT_ID", PROJECT_ID)
                .withEnv("PORT", String.valueOf(PORT))
                .waitingFor(new LogMessageWaitStrategy().withRegEx("(?s).*running.*$"));
        List<String> portBindings = new ArrayList<>();
        portBindings.add("8080:8080");
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
    @SetEnvironmentVariable(key = "FIRESTORE_EMULATOR_HOST", value = "localhost:8080")
    public void testFirestore() {
        given()
                .when().get("/firestore")
                .then()
                .statusCode(200);
    }
}
