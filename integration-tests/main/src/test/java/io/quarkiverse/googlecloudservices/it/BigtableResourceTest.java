package io.quarkiverse.googlecloudservices.it;

import static io.restassured.RestAssured.given;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.testcontainers.containers.BigtableEmulatorContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class BigtableResourceTest {
    private static final BigtableEmulatorContainer EMULATOR = new BigtableEmulatorContainer(
            DockerImageName.parse("gcr.io/google.com/cloudsdktool/cloud-sdk"));

    @BeforeAll
    public static void startGcloudContainer() {
        List<String> portBindings = new ArrayList<>();
        portBindings.add("9000:9000");
        EMULATOR.setPortBindings(portBindings);
        EMULATOR.start();
    }

    @AfterAll
    public static void stopGcloudContainer() {
        EMULATOR.stop();
    }

    @Test
    @SetEnvironmentVariable(key = "BIGTABLE_EMULATOR_HOST", value = "localhost:9000")
    public void testBigtable() {
        given()
                .when().get("/bigtable")
                .then()
                .statusCode(200);
    }
}
