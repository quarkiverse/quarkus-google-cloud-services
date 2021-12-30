package io.quarkiverse.googlecloudservices.it;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class LoggingTest {

    @Test
    public void test() throws IOException {
        RestAssured.given()
                .pathParam("payload", "World")
                .when().get("/logging/{payload}")
                .then().assertThat()
                .statusCode(200);
    }
}
