package io.quarkiverse.googlecloudservices.it;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.IsEqual.equalTo;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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

@QuarkusTest()
@TestProfile(PubSubPushResourceTest.Profile.class)
public class PubSubPushResourceTest {

    @ConfigProperty(name = "quarkus.google.cloud.project-id")
    String projectId;

    public static final class Profile implements QuarkusTestProfile {

        @Override
        public String getConfigProfile() {
            return "push";
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

            if ("unverified_email".equals(token)) {
                payload.setEmailVerified(false);
                payload.setEmail("testme@google.com");
            }

            if ("invalid_email".equals(token)) {
                payload.setEmailVerified(true);
                payload.setEmail("invalid@google.com");
            }

            if ("owner".equals(token)) {
                payload.setEmailVerified(true);
                payload.setEmail("testme@google.com");
            }

            return new GoogleIdToken(
                    header,
                    payload,
                    new byte[0],
                    new byte[0]);
        }
    }

    @Test
    public void testPubSub() {
        String message = "Hello Pub/Sub";
        given()
                .body(createMessage(message))
                .header("Authorization", "Bearer owner")
                .contentType(ContentType.JSON)
                .when().post("/pubsub-push-receiver?token=testtoken")
                .then()
                .statusCode(200);

        await().atMost(3, TimeUnit.SECONDS)
                .untilAsserted(() -> given()
                        .when().get("/pubsub-push")
                        .then()
                        .statusCode(200)
                        .body(equalTo(message)));
    }

    @Test
    public void testNoVerificationToken() {
        given()
                .body(createMessage("a"))
                .header("Authorization", "Bearer owner")
                .contentType(ContentType.JSON)
                .when().post("/pubsub-push-receiver")
                .then()
                .statusCode(401);
    }

    @Test
    public void testPushInvalidToken() {
        given()
                .body(createMessage("a"))
                .header("Authorization", "Bearer owner")
                .contentType(ContentType.JSON)
                .when().post("/pubsub-push-receiver?token=invalid_token")
                .then()
                .statusCode(401);
    }

    @Test
    public void testNoAuthHeader() {
        given()
                .body(createMessage("a"))
                .contentType(ContentType.JSON)
                .when().post("/pubsub-push-receiver?token=testtoken")
                .then()
                .statusCode(401);
    }

    @Test
    public void testInvalidAuthHeader() {
        given()
                .body(createMessage("a"))
                .header("Authorization", "Something_invalid")
                .contentType(ContentType.JSON)
                .when().post("/pubsub-push-receiver?token=testtoken")
                .then()
                .statusCode(401);
    }

    @Test
    public void testPushEmailNotVerified() {
        given()
                .body(createMessage("a"))
                .header("Authorization", "Bearer unverified_email")
                .contentType(ContentType.JSON)
                .when().post("/pubsub-push-receiver?token=testtoken")
                .then()
                .statusCode(401);
    }

    @Test
    public void testPushInvalidEmail() {
        given()
                .body(createMessage("a"))
                .header("Authorization", "Bearer invalid_email")
                .contentType(ContentType.JSON)
                .when().post("/pubsub-push-receiver?token=testtoken")
                .then()
                .statusCode(401);
    }

    private String createMessage(String message) {
        var subscriptionId = "test-push-subscription";
        var publishTime = "2021-02-26T19:13:55.749Z";
        var messageId = (int) (Math.random() * 1000);
        var data = Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8));

        return "{\n" +
                "\"subscription\": \"projects/" + projectId + "/subscriptions/" + subscriptionId + "\",\n" +
                "\"publishTime\" : \"" + publishTime + "\",\n" +
                "\"message\": {\n" +
                "\"data\": \"" + data + "\",\n" +
                "\"messageId\": \"" + messageId + "\"\n" +
                "}\n" +
                "}\n";
    }

}
