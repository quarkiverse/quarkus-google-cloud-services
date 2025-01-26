package io.quarkiverse.googlecloudservices.firebase.database;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Producer for the {@link FirebaseDatabase}.
 */
@ApplicationScoped
public class FirebaseDatabaseProducer {

    @Inject
    FirebaseApp firebaseApp;

    @Inject
    FirebaseDatabaseConfig config;

    @Produces
    @Singleton
    @Default
    public FirebaseDatabase firebaseDatabase() {
        if (config.hostOverride().isPresent()) {
            return FirebaseDatabase.getInstance(firebaseApp, config.hostOverride().get());
        } else {
            return FirebaseDatabase.getInstance(firebaseApp);
        }
    }
}
