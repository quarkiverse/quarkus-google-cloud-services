package nl.group9.testcontainers.firebase;

import java.util.function.Consumer;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.internal.EmulatorCredentials;

/**
 * Subclass of {@link FirebaseEmulatorContainer} which has some extra facilities to ease testing. Functionally
 * this class is equivalent of its superclass with respect to the testing we need to perform.
 */
public class TestableFirebaseEmulatorContainer {

    private final String name;
    private final Consumer<FirebaseOptions.Builder> options;
    private FirebaseApp app;

    /**
     * Creates a new Firebase Emulator container
     *
     * @param name The name of the firebase app (must be unique across the JVM).
     * @param options Consumer to handle additional changes to the FirebaseOptions.Builder.
     */
    public TestableFirebaseEmulatorContainer(String name, Consumer<FirebaseOptions.Builder> options) {
        this.name = name;
        this.options = options;
    }

    /**
     * Creates a new Firebase Emulator container
     *
     * @param name The name of the firebase app (must be unique across the JVM).
     */
    public TestableFirebaseEmulatorContainer(String name) {
        this.name = name;
        this.options = null;
    }

    public FirebaseEmulatorContainer.Builder testBuilder() {
        var builder = FirebaseEmulatorContainer.builder();

        /*
         * We determine the current group and user using an env variable. This is set by the GitHub Actions runner.
         * The user and group are used to set the user/group for the user in the docker container run by
         * TestContainers for the Firebase Emulators. This way, the data exported by the Firebase Emulators
         * can be read from the build.
         */
        builder.withDockerConfig()
                .withUserIdFromEnv("CURRENT_USER")
                .withGroupIdFromEnv("CURRENT_GROUP")
                .afterStart(this::afterStart)
                .done()
                .withFirebaseVersion("latest")
                .withCliArguments()
                .withProjectId("demo-test-project")
                .done();

        return builder;
    }

    private void afterStart(FirebaseEmulatorContainer container) {
        var firebaseBuilder = FirebaseOptions.builder()
                .setProjectId("demo-test-project")
                .setCredentials(new EmulatorCredentials());

        if (options != null) {
            options.accept(firebaseBuilder);
        }

        FirebaseOptions options = firebaseBuilder.build();
        app = FirebaseApp.initializeApp(options, name);
    }

    public FirebaseApp getApp() {
        return app;
    }
}
