package io.quarkiverse.googlecloudservices.it;

import static io.restassured.RestAssured.given;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import jakarta.inject.Inject;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;

@QuarkusTest
public class StorageResourceTest {
    private static final int PORT = 8089;

    private static GenericContainer<?> GCLOUD_CONTAINER;

    @Inject
    Storage storage;

    @BeforeAll
    public static void startGcloudContainer() {
        GCLOUD_CONTAINER = new GenericContainer<>(DockerImageName.parse("fsouza/fake-gcs-server"))
                .withExposedPorts(4443)
                .withCreateContainerCmdModifier(cmd -> {
                    cmd.withEntrypoint("/bin/fake-gcs-server", "-scheme", "http", "-backend", "memory");
                });
        GCLOUD_CONTAINER.setPortBindings(Collections.singletonList(String.format("%d:%d", PORT, 4443)));
        GCLOUD_CONTAINER.start();
    }

    @AfterAll
    public static void stopGcloudContainer() {
        if (GCLOUD_CONTAINER != null) {
            GCLOUD_CONTAINER.stop();
        }
    }

    @Test
    public void testStorage() {
        Bucket bucket = storage.get(StorageResource.BUCKET);
        if (bucket == null) {
            bucket = storage.create(BucketInfo.newBuilder(StorageResource.BUCKET).build());
        }
        bucket.create("hello.txt", "{\"success\": true}".getBytes(StandardCharsets.UTF_8));

        RestAssured.registerParser("text/plain", Parser.JSON);
        given()
                .when().get("/storage")
                .then()
                .statusCode(200)
                .body("success", Matchers.equalTo(true));
    }
}
