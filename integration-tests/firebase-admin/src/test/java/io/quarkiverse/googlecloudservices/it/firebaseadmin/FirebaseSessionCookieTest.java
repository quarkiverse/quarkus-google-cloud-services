package io.quarkiverse.googlecloudservices.it.firebaseadmin;

import static io.restassured.RestAssured.given;
import static io.restassured.matcher.RestAssuredMatchers.detailedCookie;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.equalTo;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@Disabled
public class FirebaseSessionCookieTest extends FirebaseAuthTest {

    @Inject
    FirebaseAuth firebaseAuth;

    @Test
    void shouldHandleRoles() throws FirebaseAuthException {
        given()
                .contentType(ContentType.JSON)
                .queryParam("uid", "6789")
                .queryParam("email", "johndoe@quarkusio.com")
                .queryParam("displayName", "John Doe")
                .post("/auth/users/create")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("uid", equalTo("6789"))
                .body("customClaims", anEmptyMap());

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("roles", Set.of("admin")))
                .put("/auth/users/{uid}/claims", 6789)
                .then()
                .log().ifValidationFails()
                .statusCode(204);

        var customToken = firebaseAuth.createCustomToken("6789");

        var emulatorHostParts = emulatorHost.split(":");
        var port = emulatorHostParts.length == 2 ? Integer.parseInt(emulatorHostParts[1]) : 9099;

        var bodyAsJson = given()
                .urlEncodingEnabled(false)
                .port(port)
                .contentType(ContentType.JSON)
                .body(Map.of("token", customToken, "returnSecureToken", true))
                .log().all()
                .post("identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken?key=test")
                .jsonPath();
        var idToken = bodyAsJson.get("idToken");

        var cookie = given()
                .header("Authorization", "Bearer " + idToken)
                .post("/session/login")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .cookie("session", detailedCookie()
                        .httpOnly(true)
                        .path("/")
                        .secured(true)
                        .sameSite("Strict"))
                .extract()
                .cookie("session");

        given()
                .cookie("session", cookie)
                .get("/app/admin-options")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("projectId", equalTo("demo-test-project-id"));

        given()
                .post("/session/logout")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .cookie("session", detailedCookie()
                        .maxAge(0)
                        .httpOnly(true)
                        .path("/")
                        .secured(true)
                        .sameSite("Strict"))
                .extract()
                .cookie("session");
    }

}
