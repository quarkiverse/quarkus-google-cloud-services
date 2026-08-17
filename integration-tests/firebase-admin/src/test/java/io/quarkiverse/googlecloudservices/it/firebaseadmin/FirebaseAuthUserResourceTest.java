package io.quarkiverse.googlecloudservices.it.firebaseadmin;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.not;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.restassured.http.ContentType;

/**
 * Base class defining tests for FirebaseAdmin which can be both run as regular tests or as integration tests.
 * The latter needed to verify native execution behaviour.
 */
public abstract class FirebaseAuthUserResourceTest extends FirebaseAuthTest {

    @Test
    void shouldCreateUser() {
        given()
                .contentType(ContentType.JSON)
                .queryParam("email", "johndoe@quarkusio.com")
                .queryParam("displayName", "John Doe")
                .post("/auth/users/create")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("uid", not(blankOrNullString()))
                .body("email", equalTo("johndoe@quarkusio.com"))
                .body("displayName", equalTo("John Doe"));
    }

    @Test
    void shouldStoreCustomUserClaims() {
        given()
                .contentType(ContentType.JSON)
                .queryParam("uid", "12345")
                .queryParam("email", "johndoe@quarkusio.com")
                .queryParam("displayName", "John Doe")
                .post("/auth/users/create")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("uid", equalTo("12345"))
                .body("customClaims", anEmptyMap());

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("role", "admin"))
                .put("/auth/users/{uid}/claims", 12345)
                .then()
                .log().ifValidationFails()
                .statusCode(204);

        given()
                .get("/auth/users/{uid}", 12345)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("customClaims", hasEntry("role", "admin"));
    }

}
