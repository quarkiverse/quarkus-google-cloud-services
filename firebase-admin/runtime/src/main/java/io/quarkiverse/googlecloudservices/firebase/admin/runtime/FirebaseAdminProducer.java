package io.quarkiverse.googlecloudservices.firebase.admin.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.internal.Utils;
import com.google.firebase.internal.EmulatorCredentials;
import com.google.firebase.internal.FirebaseProcessEnvironment;

import io.quarkiverse.googlecloudservices.common.GcpBootstrapConfiguration;
import io.quarkiverse.googlecloudservices.common.GcpConfigHolder;
import io.smallrye.config.ConfigMapping;

@ApplicationScoped
public class FirebaseAdminProducer {

    @Inject
    Instance<Credentials> googleCredentials;

    @Inject
    GcpConfigHolder gcpConfigHolder;

    @Inject
    FirebaseAuthConfig firebaseAuthConfig;

    @Produces
    @Singleton
    @Default
    public FirebaseAuth firestoreAuth(@ConfigMapping FirebaseAuthConfig firebaseAuthConfig, FirebaseApp firebaseApp) {

        // Configure the Firebase emulator to use.
        firebaseAuthConfig.emulatorHost().ifPresent(host -> FirebaseProcessEnvironment.setenv(Utils.AUTH_EMULATOR_HOST, host));

        return FirebaseAuth.getInstance(firebaseApp);
    }

    @Produces
    @Singleton
    @Default
    public FirebaseApp getFirebaseApp() {
        GcpBootstrapConfiguration gcpConfiguration = gcpConfigHolder.getBootstrapConfig();

        FirebaseOptions firebaseOptions = FirebaseOptions.builder()
                .setCredentials(googleCredentials())
                .setProjectId(gcpConfiguration.projectId().orElse(null))
                .build();

        return FirebaseApp.getApps().stream()
                .filter(app -> app.getName().equals(firebaseOptions.getProjectId()))
                .findFirst()
                .orElseGet(() -> initializeFirebaseApp(gcpConfiguration, firebaseOptions));
    }

    private GoogleCredentials googleCredentials() {
        if (firebaseAuthConfig.emulatorHost().isPresent() && firebaseAuthConfig.useEmulatorCredentials()) {
            return new EmulatorCredentials();
        } else {
            return (GoogleCredentials) googleCredentials.get();
        }
    }

    private FirebaseApp initializeFirebaseApp(GcpBootstrapConfiguration gcpBootstrapConfiguration,
            FirebaseOptions firebaseOptions) {
        return gcpBootstrapConfiguration.projectId()
                .map(appName -> FirebaseApp.initializeApp(firebaseOptions, appName))
                .orElse(FirebaseApp.initializeApp(firebaseOptions));
    }

}
