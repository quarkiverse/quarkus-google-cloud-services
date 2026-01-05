package io.quarkiverse.googlecloudservices.it.firebase;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * <p>
 * These tests validate if the dev services still work in an integration-test/shared network setup. In these
 * cases, the quarkus app is started as a container (as <code>quarkus.container-image.build = true</code>) and
 * the devservices need to be reached via a shared network connection.
 * </p>
 *
 * <p>
 * These tests minic their unit-test counterparts by design. Note that the test for firebase is in this
 * module, as the Firestore and Firebase devservices exclude each other. The other tests are in the main integration
 * test module.
 * </p>
 */
@QuarkusIntegrationTest
public class SharedNetworkIT {

    @Test
    void shouldCreateDataUsingPubsubInFirestore() {
        given()
                .body("some test string")
                .post("/app")
                .then()
                .log().ifValidationFails()
                .statusCode(204);

        given()
                .get("/app")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .assertThat()
                .body(is("some test string"));
    }
}
