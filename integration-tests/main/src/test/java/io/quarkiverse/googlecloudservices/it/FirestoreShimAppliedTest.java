package io.quarkiverse.googlecloudservices.it;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * quarkus-shim (via quarkus.shim.report=true, see application.properties) writes a summary of
 * every shim it actually wove into a class at build time. EnabledTraceUtilShim targets
 * com.google.cloud.firestore.telemetry.EnabledTraceUtil, a class in the plain third-party
 * google-cloud-firestore jar -- not a Quarkus extension, so Quarkus never Jandex-indexes it on its
 * own. Without indexing it, quarkus-shim silently never applies this patch at all (or, on a
 * quarkus-shim version whose validation is stricter, fails the build outright)
 */
@QuarkusTest
public class FirestoreShimAppliedTest {

    @Test
    public void shimReportShowsFirestoreTelemetryShimApplied() throws IOException {
        Path report = Path.of("target", "shim-report.txt");
        if (!Files.exists(report)) {
            fail("target/shim-report.txt was not written -- is quarkus.shim.report=true still set"
                    + " in application.properties?");
        }
        String content = Files.readString(report);
        assertTrue(
                content.contains("com.google.cloud.firestore.telemetry.EnabledTraceUtil")
                        && content.contains("EnabledTraceUtilShim"),
                "Expected the Firestore telemetry shim to be applied, but it wasn't found in the shim report:\n"
                        + content);
    }
}
