package io.quarkiverse.googlecloudservices.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(CustomGoogleApiAuthTest.AccessTokenProfile.class)
class CustomGoogleApiAuthTest {

    @Test
    void sendsAuthorizationHeaderFromInjectedGoogleCredentials() {
        given()
                .when().get("/custom-api")
                .then()
                .statusCode(200)
                .body("authorization", equalTo("Bearer test-token"));
    }

    public static class AccessTokenProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("integration-tests.credentials.access-token", "test-token");
        }
    }
}
