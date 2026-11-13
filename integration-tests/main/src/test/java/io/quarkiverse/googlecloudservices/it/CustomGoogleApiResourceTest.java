package io.quarkiverse.googlecloudservices.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class CustomGoogleApiResourceTest {

    @Test
    void usesInjectedNoCredentialsAndProjectId() {
        given()
                .when().get("/custom-api")
                .then()
                .statusCode(200)
                .body("path", equalTo("v1/custom"))
                .body("projectId", equalTo("test-project"))
                .body("authorization", equalTo(""));
    }
}
