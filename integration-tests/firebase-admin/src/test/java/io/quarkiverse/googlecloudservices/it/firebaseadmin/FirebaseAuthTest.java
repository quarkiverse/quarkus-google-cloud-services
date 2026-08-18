package io.quarkiverse.googlecloudservices.it.firebaseadmin;

import static io.restassured.RestAssured.given;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for authorization tests.
 * <p>
 * {@link ConfigProvider} is used to retreive the config valeus over injection as this also needs to run in a integration
 * test.
 */
public abstract class FirebaseAuthTest {

    String projectId;

    String emulatorHost;

    @BeforeEach
    public void deleteAllAccounts() {
        projectId = ConfigProvider.getConfig().getValue("quarkus.google.cloud.project-id", String.class);
        emulatorHost = ConfigProvider.getConfig().getValue("quarkus.google.cloud.firebase.auth.emulator-host",
                String.class);

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
