package io.quarkiverse.googlecloudservices.firebase.admin.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;

import io.quarkiverse.googlecloudservices.common.GcpBootstrapConfiguration;
import io.quarkiverse.googlecloudservices.common.GcpConfigHolder;

@ApplicationScoped
public class FirebaseAdminProducer {

    @Inject
    GoogleCredentials googleCredentials;

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
                .setCredentials(googleCredentials)
                .setProjectId(gcpConfiguration.projectId.orElse(null))
                .build();

        return FirebaseApp.getApps().stream()
                .filter(app -> app.getName().equals(firebaseOptions.getProjectId()))
                .findFirst()
                .orElseGet(() -> initializeFirebaseApp(gcpConfiguration, firebaseOptions));
    }

    private FirebaseApp initializeFirebaseApp(GcpBootstrapConfiguration gcpBootstrapConfiguration,
            FirebaseOptions firebaseOptions) {
        return gcpBootstrapConfiguration.projectId
                .map(appName -> FirebaseApp.initializeApp(firebaseOptions, appName))
                .orElse(FirebaseApp.initializeApp(firebaseOptions));
    }

}
