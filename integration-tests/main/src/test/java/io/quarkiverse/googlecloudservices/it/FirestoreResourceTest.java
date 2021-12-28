package io.quarkiverse.googlecloudservices.it;

import static io.restassured.RestAssured.given;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.FirestoreEmulatorContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class FirestoreResourceTest {
    private static final FirestoreEmulatorContainer EMULATOR = new FirestoreEmulatorContainer(
            DockerImageName.parse("gcr.io/google.com/cloudsdktool/cloud-sdk"));

    @BeforeAll
    public static void startGcloudContainer() {
        List<String> portBindings = new ArrayList<>();
        portBindings.add("8080:8080");
        EMULATOR.setPortBindings(portBindings);
        EMULATOR.start();
    }

    @AfterAll
    public static void stopGcloudContainer() {
        EMULATOR.stop();
    }

    @Test
    public void testFirestore() {
        given()
                .when().get("/firestore")
                .then()
                .statusCode(200);
    }
}
