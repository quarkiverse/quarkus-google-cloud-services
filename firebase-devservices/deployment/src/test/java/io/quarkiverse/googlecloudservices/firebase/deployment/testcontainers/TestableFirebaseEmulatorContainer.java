package io.quarkiverse.googlecloudservices.firebase.deployment.testcontainers;

import java.io.PrintStream;
import java.util.Optional;

import org.testcontainers.containers.output.OutputFrame;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.internal.EmulatorCredentials;

/**
 * Subclass of {@link FirebaseEmulatorContainer} which has some extra facilities to ease testing. Functionally
 * this class is equivalent of its superclass with respect to the testing we need to perform.
 */
public abstract class TestableFirebaseEmulatorContainer extends FirebaseEmulatorContainer {

    /*
     * We determine the current group and user using an env variable. This is set by the GitHub Actions runner.
     * The user and group are used to set the user/group for the user in the docker container run by
     * TestContainers for the Firebase Emulators. This way, the data exported by the Firebase Emulators
     * can be read from the build.
     */
    protected static Optional<Integer> user = Optional
            .ofNullable(System.getenv("CURRENT_USER"))
            .map(Integer::valueOf);
    protected static Optional<Integer> group = Optional
            .ofNullable(System.getenv("CURRENT_GROUP"))
            .map(Integer::valueOf);

    static {
        System.out.println("Running as user " + user + " and group " + group);
    }

    private final String name;
    private FirebaseApp app;

    /**
     * Creates a new Firebase Emulator container
     *
     * @param firebaseConfig The generic configuration of the firebase emulators
     * @param name The name of the firebase app (must be unique across the JVM).
     */
    public TestableFirebaseEmulatorContainer(EmulatorConfig firebaseConfig, String name) {
        super(firebaseConfig);
        this.name = name;
    }

    @Override
    public void start() {
        super.start();

        followOutput(this::writeToStdOut, OutputFrame.OutputType.STDOUT);
        followOutput(this::writeToStdErr, OutputFrame.OutputType.STDERR);

        var firebaseBuilder = FirebaseOptions.builder()
                .setProjectId("demo-test-project")
                .setCredentials(new EmulatorCredentials());

        createFirebaseOptions(firebaseBuilder);

        FirebaseOptions options = firebaseBuilder.build();
        app = FirebaseApp.initializeApp(options, name);
    }

    protected abstract void createFirebaseOptions(FirebaseOptions.Builder builder);

    private void writeToStdOut(OutputFrame frame) {
        writeOutputFrame(frame, System.out);
    }

    private void writeToStdErr(OutputFrame frame) {
        writeOutputFrame(frame, System.err);
    }

    private void writeOutputFrame(OutputFrame frame, PrintStream output) {
        output.println(frame.getUtf8StringWithoutLineEnding());
    }

    public FirebaseApp getApp() {
        return app;
    }
}
