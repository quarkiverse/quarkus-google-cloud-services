package io.quarkiverse.googlecloudservices.firebase.deployment;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.google.api.core.ApiFuture;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.cloud.NoCredentials;
import com.google.cloud.firestore.*;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.cloud.storage.*;
import com.google.cloud.storage.Blob;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.database.*;
import com.google.firebase.internal.EmulatorCredentials;
import com.google.firebase.internal.FirebaseProcessEnvironment;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.quarkiverse.googlecloudservices.firebase.deployment.FirebaseEmulatorContainer.Emulator;
import io.quarkiverse.googlecloudservices.firebase.deployment.FirebaseEmulatorContainer.EmulatorConfig;
import io.quarkiverse.googlecloudservices.firebase.deployment.FirebaseEmulatorContainer.ExposedPort;

@Testcontainers
public class FirebaseEmulatorContainerIntegrationTest {

    private static final FirebaseEmulatorContainer firebaseContainer;
    private static final File tempEmulatorDataDir;
    private static final File tempHostingContentDir;

    private static final String emulatorHost;
    private static final FirebaseApp app;

    private static final Map<Emulator, ExposedPort> SERVICES = Map.of(
            Emulator.AUTHENTICATION, new ExposedPort(6000),
            Emulator.REALTIME_DATABASE, new ExposedPort(6001),
            Emulator.CLOUD_FIRESTORE, new ExposedPort(6002),
            Emulator.CLOUD_FIRESTORE_WS, new ExposedPort(6003),
            Emulator.PUB_SUB, new ExposedPort(6004),
            Emulator.CLOUD_STORAGE, new ExposedPort(6005),
            //            Emulators.FIREBASE_HOSTING, new ExposedPort(6006),
            //            Emulators.CLOUD_FUNCTIONS, new ExposedPort(6007),
            //            Emulators.EVENT_ARC, new ExposedPort(6008),
            Emulator.EMULATOR_SUITE_UI, new ExposedPort(6009),
            Emulator.EMULATOR_HUB, new ExposedPort(6010),
            Emulator.LOGGING, new ExposedPort(6011));

    static {
        try {
            var user = Optional
                    .ofNullable(System.getenv("CURRENT_USER"))
                    .map(Integer::valueOf);
            var group = Optional
                    .ofNullable(System.getenv("CURRENT_GROUP"))
                    .map(Integer::valueOf);

            System.out.println("Running as user " + user + " and group " + group);

            // Create a temporary directory for emulator data
            tempEmulatorDataDir = Files.createTempDirectory("firebase-emulator-data").toFile();
            tempHostingContentDir = Files.createTempDirectory("firebase-hosting-content").toFile();

            // Create a static HTML file in the hosting directory
            File indexFile = new File(tempHostingContentDir, "index.html");
            try (FileWriter writer = new FileWriter(indexFile)) {
                writer.write("<html><body><h1>Hello, Firebase Hosting!</h1></body></html>");
            }

            /*
             * We use user id = 991 & groupid = 999 by default, as these are the ids used by the gitlab runner.
             */
            EmulatorConfig config = new EmulatorConfig(
                    new FirebaseEmulatorContainer.DockerConfig(
                            "node:23-alpine",
                            user,
                            group),
                    "latest", // Firebase version
                    Optional.of("demo-test-project"),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(tempEmulatorDataDir.toPath()),
                    Optional.of(tempHostingContentDir.toPath()),
                    SERVICES);

            firebaseContainer = new FirebaseEmulatorContainer(config);
            firebaseContainer.start();

            firebaseContainer.followOutput(FirebaseEmulatorContainerIntegrationTest::writeToStdOut,
                    OutputFrame.OutputType.STDOUT);
            firebaseContainer.followOutput(FirebaseEmulatorContainerIntegrationTest::writeToStdErr,
                    OutputFrame.OutputType.STDERR);

            emulatorHost = firebaseContainer.getHost();

            int dbPort = firebaseContainer.emulatorPort(Emulator.REALTIME_DATABASE);

            var firebaseBuilder = FirebaseOptions.builder()
                    .setProjectId("demo-test-project")
                    .setCredentials(new EmulatorCredentials())
                    .setDatabaseUrl("http://" + emulatorHost + ":" + dbPort + "?ns=demo-test-project");

            FirebaseOptions options = firebaseBuilder.build();
            app = FirebaseApp.initializeApp(options);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void writeToStdOut(OutputFrame frame) {
        writeOutputFrame(frame, System.out);
    }

    private static void writeToStdErr(OutputFrame frame) {
        writeOutputFrame(frame, System.err);
    }

    private static void writeOutputFrame(OutputFrame frame, PrintStream output) {
        output.println(frame.getUtf8StringWithoutLineEnding());
    }

    @AfterAll
    public static void tearDown() {
        if (firebaseContainer != null) {
            firebaseContainer.stop();
        }

        validateEmulatorDataWritten();

        // Recursively delete the contents of the directories and then delete the directories
        deleteDirectoryRecursively(tempEmulatorDataDir);
        deleteDirectoryRecursively(tempHostingContentDir);
    }

    // Helper method to recursively delete all files and directories
    private static void deleteDirectoryRecursively(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectoryRecursively(file);
                    } else {
                        assertTrue(file.delete());
                    }
                }
            }
            assertTrue(directory.delete());
        }
    }

    private static void validateEmulatorDataWritten() {
        var emulatorDataDir = new File(tempEmulatorDataDir, "emulator-data");
        assertTrue(emulatorDataDir.exists());
        assertTrue(emulatorDataDir.isDirectory());
        assertTrue(emulatorDataDir.canRead());
        assertTrue(emulatorDataDir.canWrite());
        assertTrue(emulatorDataDir.canExecute());

        // Verify that files were written to the emulator data directory
        File[] files = emulatorDataDir.listFiles();
        assertNotNull(files);
        assertTrue(files.length > 0, "Expected files to be present in the emulator data directory");
    }

    @Test
    @Disabled
    public void testFirebaseHostingEmulatorConnection() throws Exception {
        // Validate that the static HTML file is accessible via HTTP
        int hostingPort = firebaseContainer.emulatorPort(Emulator.FIREBASE_HOSTING);

        // Construct URL for the hosted file
        URL url = new URL("http://" + emulatorHost + ":" + hostingPort + "/index.html");

        // Fetch content from the URL
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        int responseCode = connection.getResponseCode();

        assertEquals(200, responseCode, "Expected HTTP status 200 for index.html");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line = reader.readLine();
            assertTrue(line.contains("Hello, Firebase Hosting!"), "Expected content in index.html");
        }
    }

    @Test
    public void testFirebaseAuthenticationEmulatorConnection() throws FirebaseAuthException {
        // Retrieve the host and port for the Authentication emulator
        int authPort = firebaseContainer.emulatorPort(Emulator.AUTHENTICATION);

        // Set the environment variable for the Firebase Authentication emulator
        FirebaseProcessEnvironment.setenv("FIREBASE_AUTH_EMULATOR_HOST", emulatorHost + ":" + authPort);

        // Initialize FirebaseOptions without setting the auth emulator host directly
        FirebaseAuth auth = FirebaseAuth.getInstance(app);

        // Create a test user and verify it
        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail("user@example.com")
                .setPassword("password");
        UserRecord userRecord = auth.createUser(request);

        assertNotNull(userRecord);
        assertEquals("user@example.com", userRecord.getEmail());

        // Clean up by deleting the test user
        auth.deleteUser(userRecord.getUid());
    }

    @Test
    public void testFirestoreEmulatorConnection() throws ExecutionException, InterruptedException {
        int firestorePort = firebaseContainer.emulatorPort(Emulator.CLOUD_FIRESTORE);

        FirestoreOptions options = FirestoreOptions.newBuilder()
                .setProjectId("demo-test-project")
                .setEmulatorHost(emulatorHost + ":" + firestorePort)
                .setCredentials(new EmulatorCredentials())
                .build();
        Firestore firestore = options.getService();

        DocumentReference docRef = firestore.collection("testCollection").document("testDoc");
        ApiFuture<WriteResult> result = docRef.set(Map.of("field", "value"));

        assertNotNull(result.get());
        DocumentSnapshot snapshot = docRef.get().get();
        assertEquals("value", snapshot.getString("field"));
    }

    @Test
    public void testRealtimeDatabaseEmulatorConnection() throws ExecutionException, InterruptedException {
        DatabaseReference ref = FirebaseDatabase.getInstance(app).getReference("testData");

        // Write data to the database
        ref.setValueAsync("testValue").get();

        // Set up a listener and latch for asynchronous reading
        CountDownLatch latch = new CountDownLatch(1);
        final String[] value = { null };

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                value[0] = snapshot.getValue(String.class);
                latch.countDown();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                latch.countDown();
            }
        });

        // Wait for the listener to retrieve data
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("testValue", value[0], "Expected to retrieve 'testValue' from Realtime Database");
    }

    @Test
    public void testPubSubEmulatorConnection() throws Exception {
        // Retrieve the host and port for the Pub/Sub emulator
        int pubSubPort = firebaseContainer.emulatorPort(Emulator.PUB_SUB);

        // Set up a gRPC channel to the Pub/Sub emulator
        ManagedChannel channel = ManagedChannelBuilder.forAddress(emulatorHost, pubSubPort)
                .usePlaintext()
                .build();

        // Set the channel provider for Pub/Sub client
        FixedTransportChannelProvider channelProvider = FixedTransportChannelProvider
                .create(GrpcTransportChannel.create(channel));

        TopicAdminSettings topicAdminSettings = TopicAdminSettings.newBuilder()
                .setCredentialsProvider(new NoCredentialsProvider())
                .setTransportChannelProvider(channelProvider)
                .build();

        try (TopicAdminClient topicAdminClient = TopicAdminClient.create(topicAdminSettings)) {
            topicAdminClient.createTopic("projects/demo-test-project/topics/testTopic");
        }

        // Create a publisher with the channel provider
        Publisher publisher = Publisher.newBuilder("projects/demo-test-project/topics/testTopic")
                .setChannelProvider(channelProvider)
                .setCredentialsProvider(new NoCredentialsProvider())
                .build();

        // Publish a message to the Pub/Sub emulator
        PubsubMessage message = PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8("Test message")).build();
        ApiFuture<String> messageIdFuture = publisher.publish(message);
        assertNotNull(messageIdFuture.get(), "Expected message to be published successfully");

        // Shutdown the channel
        channel.shutdownNow();
    }

    @Test
    @Disabled
    public void testStorageEmulatorConnection() {
        int storagePort = firebaseContainer.emulatorPort(Emulator.CLOUD_STORAGE);

        Storage storage = StorageOptions.newBuilder()
                .setHost("http://" + emulatorHost + ":" + storagePort)
                .setProjectId("demo-test-project")
                .setCredentials(NoCredentials.getInstance())
                .build().getService();

        var bucketName = "quarkus-hello";

        Bucket bucket = storage.get(bucketName);
        if (bucket == null) {
            bucket = storage.create(BucketInfo.newBuilder(bucketName).build());
        }
        bucket.create("hello.txt", "{\"success\": true}".getBytes(StandardCharsets.UTF_8));

        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, "test-upload").build();
        storage.create(blobInfo, "test".getBytes());

        // Verify the content of the uploaded file
        Blob blob = bucket.get("hello.txt");
        assertEquals("{\"success\": true}", new String(blob.getContent()), "Expected blob content to match");
    }

    @Test
    public void testEmulatorUIReachable() throws Exception {
        // Get the host and port for the Emulator UI
        int uiPort = firebaseContainer.emulatorPort(Emulator.EMULATOR_SUITE_UI);

        // Construct the URL for the Emulator UI root (where index.html would be served)
        URL url = new URL("http://" + emulatorHost + ":" + uiPort + "/");

        // Open a connection and send an HTTP GET request
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // Get the response code to confirm the UI is reachable
        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode, "Expected HTTP status 200 for Emulator UI index.html");

        // Close the connection
        connection.disconnect();
    }

    @Test
    public void testEmulatorHub() throws Exception {
        // Get the host and port for the Emulator UI
        int uiPort = firebaseContainer.emulatorPort(Emulator.EMULATOR_HUB);

        // Construct the URL for the Emulator UI root (where index.html would be served)
        URL url = new URL("http://" + emulatorHost + ":" + uiPort + "/emulators");

        // Open a connection and send an HTTP GET request
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // Get the response code to confirm the UI is reachable
        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode, "Expected HTTP status 200 for Emulator Hub API");

        // Close the connection
        connection.disconnect();
    }

    /*
     * CLOUD_FUNCTIONS(
     * 5001,
     * "quarkus.google.cloud.functions.emulator-host",
     * "functions"),
     * EVENT_ARC(
     * 9299,
     * "quarkus.google.cloud.eventarc.emulator-host",
     * "eventarc"),
     */
}
