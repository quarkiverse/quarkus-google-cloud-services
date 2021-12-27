package io.quarkiverse.googlecloudservices.it;

import static io.restassured.RestAssured.given;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import com.google.cloud.spanner.Spanner;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class SpannerResourceTest {
    private static final int GRPC_PORT = 9010;
    private static final int HTTP_PORT = 9020;

    private static GenericContainer<?> GCLOUD_CONTAINER;

    @Inject
    Spanner spanner;

    @BeforeAll
    public static void startGcloudContainer() {
        GCLOUD_CONTAINER = new GenericContainer<>("roryq/spanner-emulator")
                .withExposedPorts(GRPC_PORT)
                .withExposedPorts(HTTP_PORT)
                .withEnv("SPANNER_DATABASE_ID", "test-database")
                .withEnv("SPANNER_INSTANCE_ID", "test-instance")
                .withEnv("SPANNER_PROJECT_ID", "test-project")
                .waitingFor(new LogMessageWaitStrategy().withRegEx("(?s).*gRPC server listening.*$"));
        List<String> portBindings = new ArrayList<>();
        portBindings.add("9010:9010");
        portBindings.add("9020:9020");
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
    public void testSpanner() throws ExecutionException, InterruptedException, TimeoutException {
        // create the table DDL
        spanner.getDatabaseAdminClient()
                .updateDatabaseDdl("test-instance", "test-database",
                        Arrays.asList(
                                "CREATE TABLE Singers ( SingerId INT64 NOT NULL, FirstName STRING(1024), LastName STRING(1024), SingerInfo BYTES(MAX) ) PRIMARY KEY (SingerId)"),
                        null)
                .get(1, TimeUnit.SECONDS);

        given()
                .when().get("/spanner")
                .then()
                .statusCode(200);
    }
}
