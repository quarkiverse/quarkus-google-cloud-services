package io.quarkiverse.googlecloudservices.it.firebase;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@Disabled
public class FirebaseResourceTest {

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
