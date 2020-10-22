package io.quarkiverse.googlecloudservices.it;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import io.quarkus.test.junit.NativeImageTest;

@NativeImageTest
@EnabledIfSystemProperty(named = "gcloud.test", matches = "true")
public class NativeGoogleServicesResourcesIT extends GoogleServicesResourcesTest {
    // Execute the same tests but in native mode.
}
