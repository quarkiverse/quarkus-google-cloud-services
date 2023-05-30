package io.quarkiverse.googlecloudservices.it;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.IsEqual.equalTo;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class PubSubResourceTest {

    @Test
    public void testPubSub() throws ExecutionException, InterruptedException, TimeoutException {
        String message = "Hello Pub/Sub";
        given()
                .body(message)
                .contentType(ContentType.TEXT)
                .when().post("/pubsub")
                .then()
                .statusCode(204);

        await().atMost(3, TimeUnit.SECONDS)
                .untilAsserted(() -> given()
                        .when().get("/pubsub")
                        .then()
                        .statusCode(200)
                        .body(equalTo(message)));
    }
}
