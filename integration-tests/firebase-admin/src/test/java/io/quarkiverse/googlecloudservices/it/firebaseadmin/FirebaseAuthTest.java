package io.quarkiverse.googlecloudservices.it.firebaseadmin;

import static io.restassured.RestAssured.given;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;

public abstract class FirebaseAuthTest {

    @ConfigProperty(name = "quarkus.google.cloud.project-id")
    String projectId;

    @ConfigProperty(name = "quarkus.google.cloud.firebase.auth.emulator-host")
    String emulatorHost;

    @BeforeEach
    public void deleteAllAccounts() {
        var emulatorHostParts = emulatorHost.split(":");
        var port = emulatorHostParts.length == 2 ? Integer.parseInt(emulatorHostParts[1]) : 9099;

        given()
                .port(port)
                .auth()
                .oauth2("owner")
                .delete("/emulator/v1/projects/{projectId}/accounts", projectId)
                .then()
                .statusCode(200);
    }

}
