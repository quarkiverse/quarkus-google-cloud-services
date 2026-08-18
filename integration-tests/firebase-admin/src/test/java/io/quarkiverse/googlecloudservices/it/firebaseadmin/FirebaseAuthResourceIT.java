package io.quarkiverse.googlecloudservices.it.firebaseadmin;

import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * Integration test version of {@link FirebaseAuthUserResourceTest}. Runs to validate the native-image
 * fixes for FirebaseAdmin in a native container.
 */
@QuarkusIntegrationTest
class FirebaseAuthResourceIT extends FirebaseAuthUserResourceTest {
}
