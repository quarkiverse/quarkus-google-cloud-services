package io.quarkiverse.googlecloudservices.it.accesstoken;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(RecordingBigQueryServer.class)
class BigQueryAccessTokenTest {

    private List<String> captured() throws Exception {
        String base = ConfigProvider.getConfig()
                .getValue("quarkus.google.cloud.bigquery.host-override", String.class);
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(base + "/_captured")).build(),
                HttpResponse.BodyHandlers.ofString());
        return response.body().lines().toList();
    }

    @Test
    void withoutBearerIsUnauthorized() {
        given().when().get("/datasets").then().statusCode(401);
    }

    @Test
    void forwardsBearerToken() throws Exception {
        given().header("Authorization", "Bearer test-token").get("/datasets").then().statusCode(200);
        assertEquals(List.of("Bearer test-token"), captured());
    }
}
