package io.quarkiverse.googlecloudservices.it;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.IsEqual.equalTo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PubSubEmulatorContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class PubSubResourceTest {
    private static final PubSubEmulatorContainer EMULATOR = new PubSubEmulatorContainer(
            DockerImageName.parse("gcr.io/google.com/cloudsdktool/cloud-sdk"));

    @BeforeAll
    public static void startGcloudContainer() {
        List<String> portBindings = new ArrayList<>();
        portBindings.add("8085:8085");
        EMULATOR.setPortBindings(portBindings);
        EMULATOR.start();
    }

    @AfterAll
    public static void stopGcloudContainer() {
        EMULATOR.stop();
    }

    @Test
    public void testPubSub() throws ExecutionException, InterruptedException, TimeoutException {
        String message = "Hello Pub/Sub";
        given()
                .body(message)
                .contentType(ContentType.TEXT)
                .when().post("/pubsub")
                .then()
                .statusCode(204);

        await().atMost(3, TimeUnit.SECONDS)
                .untilAsserted(() -> given()
                        .when().get("/pubsub")
                        .then()
                        .statusCode(200)
                        .body(equalTo(message)));
    }
}
