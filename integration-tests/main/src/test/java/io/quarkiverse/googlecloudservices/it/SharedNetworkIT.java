package io.quarkiverse.googlecloudservices.it;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.IsEqual.equalTo;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.http.ContentType;

/**
 * <p>
 * These tests validate if the dev services still work in an integration-test/shared network setup. In these
 * cases, the quarkus app is started as a container (as <code>quarkus.container-image.build = true</code>) and
 * the devservices need to be reached via a shared network connection.
 * </p>
 *
 * <p>
 * These tests minic their unit-test counterparts by design. Note that the test for firebase is in the firebase
 * module, as the Firestore and Firebase devservices exclude each other
 * </p>
 */
@QuarkusIntegrationTest
public class SharedNetworkIT {

    @Test
    public void testSharedNetworkBigTable() {
        given()
                .when().get("/bigtable")
                .then()
                .statusCode(200);
    }

    @Test
    public void testSharedNetworkFirestore() {
        given()
                .when().get("/firestore/query")
                .then()
                .statusCode(200);
    }

    @Test
    public void testSharedNetworkPubSub() {
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

    @Test
    public void testSharedNetworkSpanner() {
        given()
                .when().post("/spanner")
                .then()
                .statusCode(204);

        given()
                .when().get("/spanner")
                .then()
                .statusCode(200);

    }
}
