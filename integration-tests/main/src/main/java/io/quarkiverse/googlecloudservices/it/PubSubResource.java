package io.quarkiverse.googlecloudservices.it;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import org.jboss.logging.Logger;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;

import io.quarkiverse.googlecloudservices.it.pubsub.TopicManager;

@Path("/pubsub")
public class PubSubResource {
    private static final Logger LOG = Logger.getLogger(PubSubResource.class);

    @Inject
    TopicManager topicManager;

    private Subscriber subscriber;
    private String lastMessage;

    @PostConstruct
    void init() throws IOException {
        // subscribe to PubSub
        MessageReceiver receiver = (message, consumer) -> {
            this.lastMessage = message.getData().toStringUtf8();
            LOG.infov("Got message {0}", this.lastMessage);
            consumer.ack();
        };
        subscriber = topicManager.initSubscriber(receiver);
        subscriber.startAsync().awaitRunning();
    }

    @PreDestroy
    void destroy() {
        if (subscriber != null) {
            subscriber.stopAsync();
        }
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void sendMessage(String message) throws IOException, InterruptedException {
        Publisher publisher = topicManager.initPublisher();
        try {
            ByteString data = ByteString.copyFromUtf8(message);
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();
            ApiFuture<String> messageIdFuture = publisher.publish(pubsubMessage);
            ApiFutures.addCallback(messageIdFuture, new ApiFutureCallback<String>() {
                public void onSuccess(String messageId) {
                    LOG.infov("published with message id {0}", messageId);
                }

                public void onFailure(Throwable t) {
                    LOG.warnv("failed to publish: {0}", t);
                }
            }, MoreExecutors.directExecutor());
        } finally {
            publisher.shutdown();
            publisher.awaitTermination(1, TimeUnit.MINUTES);
        }
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String verifyMessage() {
        return this.lastMessage;
    }
}
