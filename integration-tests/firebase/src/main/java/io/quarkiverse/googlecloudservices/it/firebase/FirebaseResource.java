package io.quarkiverse.googlecloudservices.it.firebase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;

import io.quarkiverse.googlecloudservices.pubsub.QuarkusPubSub;
import io.quarkus.runtime.StartupEvent;

@Path("/app")
public class FirebaseResource {

    @Inject
    Firestore firestore;

    @Inject
    QuarkusPubSub quarkusPubSub;

    Publisher publisher;

    public void init(@Observes StartupEvent event) throws IOException {
        quarkusPubSub.createTopic("test");
        quarkusPubSub.createSubscription("test", "test");
        publisher = quarkusPubSub.publisher("test");
    }

    @POST
    public void createData(String data) throws InterruptedException, ExecutionException {
        Object monitor = new Object();

        var subscriber = quarkusPubSub.subscriber("test", (message, consumer) -> {
            try {
                var col = firestore.collection("test");
                var msgData = message.getData().toString(StandardCharsets.UTF_8);
                var fields = Map.of("test", msgData);

                col.document("test").create(fields).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

            consumer.ack();

            synchronized (monitor) {
                monitor.notify();
            }
        });
        subscriber.startAsync().awaitRunning();

        publisher.publish(PubsubMessage
                .newBuilder()
                .setData(ByteString.copyFrom(data, StandardCharsets.UTF_8))
                .build()).get();
        synchronized (monitor) {
            monitor.wait(5000);
        }

        subscriber.stopAsync().awaitTerminated();
    }

    @GET
    public Response getData() throws ExecutionException, InterruptedException {
        var col = firestore.collection("test");
        var docs = col.listDocuments();
        var iter = docs.iterator();

        if (iter.hasNext()) {
            var docRef = iter.next();
            var snapshot = docRef.get().get();
            return Response.ok(snapshot.getString("test")).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

    }
}
