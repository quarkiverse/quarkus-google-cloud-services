package io.quarkiverse.googlecloudservices.it;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

/**
 * Runs in its own restart group, as the test profile changes the configuration.
 *
 * @see PubSubDevServiceReuseTest
 */
@QuarkusTest
@TestProfile(PubSubDevServiceReuseProfileTest.Profile.class)
public class PubSubDevServiceReuseProfileTest {

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            // Unrelated to the Dev Services, a restart group is created for any configuration change
            return Map.of("quarkus.log.category.\"io.quarkiverse\".level", "DEBUG");
        }
    }

    @Test
    void shouldRunASingleEmulator() {
        PubSubDevServiceReuseTest.assertSingleRunningEmulator();
    }
}
