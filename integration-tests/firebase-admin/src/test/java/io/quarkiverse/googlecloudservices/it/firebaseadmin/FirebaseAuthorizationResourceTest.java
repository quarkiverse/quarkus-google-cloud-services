package io.quarkiverse.googlecloudservices.it.firebaseadmin;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@Disabled
public class FirebaseAuthorizationResourceTest {

    @Test
    void shouldNotReturnOptions() {
        given()
                .get("/app/secret-options")
                .then()
                .log().ifValidationFails()
                .statusCode(401);
    }

    @Test
    void shouldReturnOptions() {
        given()
                .get("/app/options")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("projectId", equalTo("demo-test-project-id"));
    }
}
