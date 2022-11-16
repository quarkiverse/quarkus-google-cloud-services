package io.quarkiverse.googlecloudservices.it.firebaseadmin;

import static io.restassured.RestAssured.given;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;

public abstract class FirebaseAuthTest {

    @ConfigProperty(name = "quarkus.google.cloud.project-id")
    String projectId;

    @BeforeEach
    public void deleteAllAccounts() {
        given()
                .port(9099)
                .auth()
                .oauth2("owner")
                .delete("http://localhost:9099/emulator/v1/projects/{projectId}/accounts", projectId)
                .then()
                .statusCode(200);
    }

}
