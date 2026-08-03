package io.quarkiverse.googlecloudservices.it.firebase;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * Disables {@code expose-to-companion-containers}, for {@link CompanionContainerDisabledTest}. Also points at a
 * dedicated {@code firebase.json} fixture with its own port range, distinct from the one the other tests in this
 * module share (see {@code src/test/resources/firebase-companion-disabled.json}). This config change forces the
 * shared dev-services container to be stopped and restarted for this profile; giving it different ports avoids a
 * race between the old container releasing its ports and the new one claiming them, rather than depending on the
 * two happening to be sequenced cleanly.
 */
public class NoCompanionContainerExposureProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.google.cloud.devservices.firebase.emulator.expose-to-companion-containers", "false",
                "quarkus.google.cloud.devservices.firebase.emulator.custom-firebase-json",
                "src/test/resources/firebase-companion-disabled.json");
    }
}
