package io.quarkiverse.googlecloudservices.it;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.IsEqual.equalTo;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.pubsub.v1.PubsubMessage;

import io.quarkiverse.googlecloudservices.pubsub.QuarkusPubSub;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest()
@TestProfile(PubSubPushResourceE2ETest.Profile.class)
public class PubSubPushResourceE2ETest {

    @Inject
    QuarkusPubSub pubSub;

    public static final class Profile implements QuarkusTestProfile {

        @Override
        public String getConfigProfile() {
            return "push-e2e";
        }

    }

    @BeforeEach
    public void setup() throws IOException {
        pubSub.createTopicsAndPushSubscriptions(Map.of("test-push-topic", List.of("test-push-subscription")));
    }

    @Test
    public void testPushUsingPublisher() throws IOException {
        String message = "Hello Pub/Sub";

        var publisher = pubSub.publisher("test-push-topic");
        var pubsubMessage = PubsubMessage.newBuilder()
                .setData(com.google.protobuf.ByteString.copyFromUtf8(message))
                .build();

        publisher.publish(pubsubMessage);

        await().atMost(3, TimeUnit.SECONDS)
                .untilAsserted(() -> given()
                        .when().get("/pubsub-push")
                        .then()
                        .statusCode(200)
                        .body(equalTo(message)));
    }

}
