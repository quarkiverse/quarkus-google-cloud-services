package io.quarkiverse.googlecloudservices.it;

import static io.restassured.RestAssured.given;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class SpannerResourceTest {

    @Test
    public void testSpanner() throws ExecutionException, InterruptedException, TimeoutException {
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
