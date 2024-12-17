package io.quarkiverse.googlecloudservices.firebase.deployment.testcontainers;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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

@Testcontainers
public class FirebaseEmulatorContainerIntegrationTest {

    private static final File tempEmulatorDataDir;
    private static final File tempHostingContentDir;

    static {
        try {
            // Create a temporary directory for emulator data
            tempEmulatorDataDir = Files.createTempDirectory("firebase-emulator-data").toFile();
            tempHostingContentDir = Files.createTempDirectory("firebase-hosting-content").toFile();

            // Create a static HTML file in the hosting directory
            File indexFile = new File(tempHostingContentDir, "index.html");
            try (FileWriter writer = new FileWriter(indexFile, Charset.defaultCharset())) {
                writer.write("<html><body><h1>Hello, Firebase Hosting!</h1></body></html>");
            }

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final TestableFirebaseEmulatorContainer testContainer = new TestableFirebaseEmulatorContainer(
            "FirebaseEmulatorContainerIntegrationTest",
            FirebaseEmulatorContainerIntegrationTest::customizeFirebaseOptions);

    private static final FirebaseEmulatorContainer firebaseContainer = testContainer.testBuilder()
            .withCliArguments()
            .withEmulatorData(tempEmulatorDataDir.toPath())
            .done()
            .withFirebaseConfig()
            .withHostingPath(tempHostingContentDir.toPath())
            .withFunctionsFromPath(new File("src/test/functions").toPath())
            .withEmulatorsOnPorts(
                    FirebaseEmulatorContainer.Emulator.AUTHENTICATION, 6000,
                    FirebaseEmulatorContainer.Emulator.REALTIME_DATABASE, 6001,
                    FirebaseEmulatorContainer.Emulator.CLOUD_FIRESTORE, 6002,
                    FirebaseEmulatorContainer.Emulator.CLOUD_FIRESTORE_WS, 6003,
                    FirebaseEmulatorContainer.Emulator.PUB_SUB, 6004,
                    FirebaseEmulatorContainer.Emulator.CLOUD_STORAGE, 6005,
                    FirebaseEmulatorContainer.Emulator.FIREBASE_HOSTING, 6006,
                    FirebaseEmulatorContainer.Emulator.CLOUD_FUNCTIONS, 6007,
                    //            Emulator.EVENT_ARC, 6008,
                    FirebaseEmulatorContainer.Emulator.EMULATOR_SUITE_UI, 6009,
                    FirebaseEmulatorContainer.Emulator.EMULATOR_HUB, 6010,
                    FirebaseEmulatorContainer.Emulator.LOGGING, 6011)
            .done()
            .build();

    private static void customizeFirebaseOptions(FirebaseOptions.Builder builder) {
        var emulatorHost = firebaseContainer.getHost();
        var dbPort = firebaseContainer.emulatorPort(FirebaseEmulatorContainer.Emulator.REALTIME_DATABASE);

        builder.setDatabaseUrl("http://" + emulatorHost + ":" + dbPort + "?ns=demo-test-project");
    }

    @BeforeAll
    public static void setup() {
        firebaseContainer.start();
    }

    @AfterAll
    public static void tearDown() {
        firebaseContainer.stop();

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
    public void testFirebaseAuthenticationEmulatorConnection() throws FirebaseAuthException {
        // Retrieve the host and port for the Authentication emulator
        int authPort = firebaseContainer.emulatorPort(FirebaseEmulatorContainer.Emulator.AUTHENTICATION);

        // Set the environment variable for the Firebase Authentication emulator
        FirebaseProcessEnvironment.setenv("FIREBASE_AUTH_EMULATOR_HOST", firebaseContainer.getHost() + ":" + authPort);

        // Initialize FirebaseOptions without setting the auth emulator host directly
        FirebaseAuth auth = FirebaseAuth.getInstance(testContainer.getApp());

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
    public void testFirestoreEmulatorConnection() throws Exception {
        int firestorePort = firebaseContainer.emulatorPort(FirebaseEmulatorContainer.Emulator.CLOUD_FIRESTORE);

        FirestoreOptions options = FirestoreOptions.newBuilder()
                .setProjectId("demo-test-project")
                .setEmulatorHost(firebaseContainer.getHost() + ":" + firestorePort)
                .setCredentials(new EmulatorCredentials())
                .build();

        try (Firestore firestore = options.getService()) {
            DocumentReference docRef = firestore.collection("testCollection").document("testDoc");
            ApiFuture<WriteResult> result = docRef.set(Map.of("field", "value"));

            assertNotNull(result.get());
            DocumentSnapshot snapshot = docRef.get().get();
            assertEquals("value", snapshot.getString("field"));
        }
    }

    @Test
    public void testRealtimeDatabaseEmulatorConnection() throws ExecutionException, InterruptedException {
        DatabaseReference ref = FirebaseDatabase.getInstance(testContainer.getApp()).getReference("testData");

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
        int pubSubPort = firebaseContainer.emulatorPort(FirebaseEmulatorContainer.Emulator.PUB_SUB);

        // Set up a gRPC channel to the Pub/Sub emulator
        ManagedChannel channel = ManagedChannelBuilder.forAddress(firebaseContainer.getHost(), pubSubPort)
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
    public void testStorageEmulatorConnection() throws IOException {
        int storagePort = firebaseContainer.emulatorPort(FirebaseEmulatorContainer.Emulator.CLOUD_STORAGE);

        Storage storage = StorageOptions.newBuilder()
                .setHost("http://" + firebaseContainer.getHost() + ":" + storagePort)
                .setProjectId("demo-test-project")
                .setCredentials(NoCredentials.getInstance())
                .build().getService();

        var bucketName = "demo-test-project.appspot.com";

        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, "test-upload")
                .setContentType("application/json")
                .setContentDisposition("attachment; filename=\"test-upload\"")
                .build();

        try (var writer = storage.writer(blobInfo)) {
            writer.write(ByteBuffer.wrap("{\"test\": 1}".getBytes(StandardCharsets.UTF_8)));
        }

        try (var reader = storage.reader(blobInfo.getBlobId())) {
            try (var bufReader = new BufferedReader(Channels.newReader(reader, StandardCharsets.UTF_8))) {
                var contents = bufReader.readLine();
                assertEquals("{\"test\": 1}", contents, "Expected blob content to match");
            }
        }
    }

    @Test
    public void testEmulatorUIReachable() throws Exception {
        // Get the host and port for the Emulator UI
        int uiPort = firebaseContainer.emulatorPort(FirebaseEmulatorContainer.Emulator.EMULATOR_SUITE_UI);

        // Construct the URL for the Emulator UI root (where index.html would be served)
        URL url = new URI("http://" + firebaseContainer.getHost() + ":" + uiPort + "/").toURL();

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
        int uiPort = firebaseContainer.emulatorPort(FirebaseEmulatorContainer.Emulator.EMULATOR_HUB);

        // Construct the URL for the Emulator UI root (where index.html would be served)
        URL url = new URI("http://" + firebaseContainer.getHost() + ":" + uiPort + "/emulators").toURL();

        // Open a connection and send an HTTP GET request
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // Get the response code to confirm the UI is reachable
        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode, "Expected HTTP status 200 for Emulator Hub API");

        // Close the connection
        connection.disconnect();
    }

    @Test
    public void testHosting() throws IOException, InterruptedException, URISyntaxException {
        HttpClient httpClient = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
                .GET()
                .uri(new URI("http://localhost:6006/index.html"))
                .build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        var body = response.body();
        assertEquals("<html><body><h1>Hello, Firebase Hosting!</h1></body></html>", body);
    }

    @Test
    public void testFunctions() throws IOException, InterruptedException, URISyntaxException {
        HttpClient httpClient = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
                .GET()
                .uri(new URI("http://localhost:6007/demo-test-project/us-central1/helloworld"))
                .build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        var body = response.body();
        assertEquals("Hello world", body);
    }

}
