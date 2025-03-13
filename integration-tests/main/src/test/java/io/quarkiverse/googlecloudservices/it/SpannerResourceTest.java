package io.quarkiverse.googlecloudservices.it;

import static io.restassured.RestAssured.given;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import com.google.cloud.spanner.InstanceConfigId;
import com.google.cloud.spanner.InstanceId;
import com.google.cloud.spanner.InstanceInfo;
import com.google.cloud.spanner.Spanner;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class SpannerResourceTest {

    @Inject
    Spanner spanner;

    @Test
    public void testSpanner() throws ExecutionException, InterruptedException, TimeoutException {
        // create the instance
        spanner.getInstanceAdminClient().createInstance(
                InstanceInfo.newBuilder(InstanceId.of("test-project", "test-instance"))
                        .setInstanceConfigId(InstanceConfigId.of("test-project", "test-config")).build())
                .get(1, TimeUnit.SECONDS);

        // create the database and the table
        spanner.getDatabaseAdminClient()
                .createDatabase("test-instance", "test-database", List.of(
                        "CREATE TABLE Singers ( SingerId INT64 NOT NULL, FirstName STRING(1024), LastName STRING(1024), SingerInfo BYTES(MAX) ) PRIMARY KEY (SingerId)"))
                .get(1, TimeUnit.SECONDS);

        given()
                .when().get("/spanner")
                .then()
                .statusCode(200);
    }
}
