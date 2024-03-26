package io.quarkiverse.googlecloudservices.it;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class FirestoreResourceTest {

    @Test
    public void testFirestoreQuery() {
        given()
                .when().get("/firestore/query")
                .then()
                .statusCode(200);
    }

    @Test
    public void testFirestoreListDocuments() {
        given()
                .when().get("/firestore/all")
                .then()
                .statusCode(200);
    }
}
