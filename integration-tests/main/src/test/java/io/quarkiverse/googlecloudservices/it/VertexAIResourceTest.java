package io.quarkiverse.googlecloudservices.it;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@Disabled
class VertexAIResourceTest {
    @Test
    public void testVertxAI() {
        given()
                .when().get("/vertexai?prompt=Hello%20World")
                .then()
                .statusCode(200);
    }
}
