package io.quarkiverse.googlecloudservices.firebase.admin.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;

import io.quarkiverse.googlecloudservices.common.GcpBootstrapConfiguration;
import io.quarkiverse.googlecloudservices.common.GcpConfigHolder;

@ApplicationScoped
public class FirebaseAdminProducer {

    @Inject
    Credentials googleCredentials;

    @Inject
    GcpConfigHolder gcpConfigHolder;

    @Produces
    @Singleton
    @Default
    public FirebaseAuth firestoreAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    @Produces
    @Singleton
    @Default
    public FirebaseApp getFirebaseApp() {
        GcpBootstrapConfiguration gcpConfiguration = gcpConfigHolder.getBootstrapConfig();

        FirebaseOptions firebaseOptions = FirebaseOptions.builder()
                .setCredentials((GoogleCredentials) googleCredentials)
                .setProjectId(gcpConfiguration.projectId().orElse(null))
                .build();

        return FirebaseApp.getApps().stream()
                .filter(app -> app.getName().equals(firebaseOptions.getProjectId()))
                .findFirst()
                .orElseGet(() -> initializeFirebaseApp(gcpConfiguration, firebaseOptions));
    }

    private FirebaseApp initializeFirebaseApp(GcpBootstrapConfiguration gcpBootstrapConfiguration,
            FirebaseOptions firebaseOptions) {
        return gcpBootstrapConfiguration.projectId()
                .map(appName -> FirebaseApp.initializeApp(firebaseOptions, appName))
                .orElse(FirebaseApp.initializeApp(firebaseOptions));
    }

}
