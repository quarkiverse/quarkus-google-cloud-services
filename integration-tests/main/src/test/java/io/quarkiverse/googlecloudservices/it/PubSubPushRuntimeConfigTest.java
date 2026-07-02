package io.quarkiverse.googlecloudservices.it;

import static io.restassured.RestAssured.given;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

import javax.annotation.Priority;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.json.webtoken.JsonWebSignature;

import io.quarkiverse.googlecloudservices.pubsub.push.TokenVerifier;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

/**
 * Verifies that quarkus.google.cloud.pubsub.push.service-account-email resolves correctly
 * when the value is supplied only at runtime (e.g. via an environment variable), and is
 * absent from application.properties at build time.
 *
 * This is a regression test for the BUILD_AND_RUN_TIME_FIXED → RUN_TIME config migration:
 * with the old phase, the value was baked in at augmentation and a deploy-time env var
 * was silently ignored; with RUN_TIME it is always read from the live config system.
 */
@QuarkusTest
@TestProfile(PubSubPushRuntimeConfigTest.Profile.class)
public class PubSubPushRuntimeConfigTest {

    @ConfigProperty(name = "quarkus.google.cloud.project-id")
    String projectId;

    public static final class Profile implements QuarkusTestProfile {

        @Override
        public String getConfigProfile() {
            return "push-runtime-config";
        }

        /**
         * Supplies service-account-email at "runtime" only — not present in application.properties.
         * This simulates an environment variable (e.g. QUARKUS_GOOGLE_CLOUD_PUBSUB_PUSH_SERVICE_ACCOUNT_EMAIL)
         * that is injected at deploy time but not available during the build.
         */
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.google.cloud.pubsub.push.service-account-email", "runtime@google.com");
        }

        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of(MockTokenVerifier.class);
        }
    }

    @Alternative
    @ApplicationScoped
    @Priority(1)
    public static final class MockTokenVerifier implements TokenVerifier {

        @Override
        public GoogleIdToken verify(String token) {
            var header = new JsonWebSignature.Header();
            var payload = new GoogleIdToken.Payload();

            if ("valid-token".equals(token)) {
                // email matches the value supplied via getConfigOverrides() — should be accepted
                payload.setEmailVerified(true);
                payload.setEmail("runtime@google.com");
            } else if ("wrong-email".equals(token)) {
                // email does not match — should be rejected even if token is otherwise valid
                payload.setEmailVerified(true);
                payload.setEmail("other@google.com");
            }
            // any other token → empty payload → missing email → rejected

            return new GoogleIdToken(header, payload, new byte[0], new byte[0]);
        }
    }

    @Test
    public void testRuntimeServiceAccountEmailIsAccepted() {
        // The service-account-email "runtime@google.com" was supplied only via getConfigOverrides(),
        // not in application.properties. If the config were BUILD_AND_RUN_TIME_FIXED this would fail
        // because the build-time value would be empty and the filter would throw on orElseThrow().
        given()
                .body(createMessage("hello"))
                .header("Authorization", "Bearer valid-token")
                .contentType(ContentType.JSON)
                .when().post("/pubsub-push-receiver?token=testtoken")
                .then()
                .statusCode(200);
    }

    @Test
    public void testWrongServiceAccountEmailIsRejected() {
        given()
                .body(createMessage("hello"))
                .header("Authorization", "Bearer wrong-email")
                .contentType(ContentType.JSON)
                .when().post("/pubsub-push-receiver?token=testtoken")
                .then()
                .statusCode(401);
    }

    @Test
    public void testMissingAuthHeaderIsRejected() {
        given()
                .body(createMessage("hello"))
                .contentType(ContentType.JSON)
                .when().post("/pubsub-push-receiver?token=testtoken")
                .then()
                .statusCode(401);
    }

    private String createMessage(String message) {
        var data = Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8));
        return "{\n" +
                "\"subscription\": \"projects/" + projectId + "/subscriptions/test-push-subscription\",\n" +
                "\"publishTime\" : \"2021-02-26T19:13:55.749Z\",\n" +
                "\"message\": {\n" +
                "\"data\": \"" + data + "\",\n" +
                "\"messageId\": \"1\"\n" +
                "}\n" +
                "}\n";
    }
}
