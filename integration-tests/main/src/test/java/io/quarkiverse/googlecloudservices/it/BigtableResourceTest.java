package io.quarkiverse.googlecloudservices.it;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class BigtableResourceTest {

    @Test
    public void testBigtable() {
        given()
                .when().get("/bigtable")
                .then()
                .statusCode(200);
    }
}
