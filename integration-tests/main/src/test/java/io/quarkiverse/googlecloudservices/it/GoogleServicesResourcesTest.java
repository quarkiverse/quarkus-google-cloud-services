package io.quarkiverse.googlecloudservices.it;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;

import java.util.concurrent.TimeUnit;

import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@EnabledIfSystemProperty(named = "gcloud.test", matches = "true")
public class GoogleServicesResourcesTest {

    @Test
    public void testBigQuery() {
        given()
                .when().get("/bigquery")
                .then()
                .statusCode(200);
    }

    @Test
    public void testBigtable() {
        given()
                .when().get("/bigtable")
                .then()
                .statusCode(200);
    }

    @Test
    public void testFirestore() {
        given()
                .when().get("/firestore")
                .then()
                .statusCode(200);
    }

    @Test
    public void testPubSub() {
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
                        .body(IsEqual.equalTo(message)));
    }

    @Test
    public void testSpanner() {
        given()
                .when().get("/spanner")
                .then()
                .statusCode(200);
    }

    @Test
    public void testStorage() {
        given()
                .when().get("/storage")
                .then()
                .statusCode(200);
    }

    @Test
    public void testSecretManager() {
        given()
                .when().get("/secretmanager")
                .then()
                .statusCode(200)
                .body(equalTo("Secret accessed via client: hello || Secret accessed via property: hello"));
    }
}
