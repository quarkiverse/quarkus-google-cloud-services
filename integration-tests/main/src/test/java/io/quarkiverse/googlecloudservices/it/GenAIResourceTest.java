package io.quarkiverse.googlecloudservices.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class GenAIResourceTest {
    @Test
    public void testGenAI() {
        given()
                .when().get("/genai?prompt=Hello%20World")
                .then()
                .statusCode(200)
                .body(equalTo("Hello from the stub"));
    }
}
