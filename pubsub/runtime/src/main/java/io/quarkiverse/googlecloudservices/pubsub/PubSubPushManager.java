package io.quarkiverse.googlecloudservices.pubsub;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import io.vertx.ext.web.handler.BodyHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.google.api.core.AbstractApiService;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.SubscriberInterface;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * This class manages pub-sub push subscriptions (see <a href="https://cloud.google.com/pubsub/docs/push">here</a>).
 * <p>
 * It provides a way to subscribe to messages (via {@link QuarkusPubSub} comparable to regular pull subscriptions. Also
 * handlers are implemented to handle the incoming HTTP connections.
 * <p>
 * Users can opt to use the default routes, or provide their own logic using the provided utility methods to handle these
 * requests.
 */
@ApplicationScoped
public class PubSubPushManager {

    private static final Logger LOGGER = Logger.getLogger(PubSubPushManager.class.getName());

    private final ConcurrentHashMap<ProjectSubscriptionName, MessageReceiver> messageReceivers = new ConcurrentHashMap<>();

    @ConfigProperty(name = "quarkus.google.cloud.pubsub.push.enabled")
    boolean enabled;

    @ConfigProperty(name = "quarkus.google.cloud.pubsub.push.endpoint-path")
    Optional<String> pubSubEndpoint;

    /**
     * Register the push route for pubsub
     *
     * @param router The vertx router
     */
    public void setupRouters(@Observes Router router) {
        if (!enabled) {
            return;
        }

        pubSubEndpoint.ifPresent(endpoint -> router.post(endpoint)
                .consumes("application/json")
                .handler(BodyHandler.create())
                .handler(this::handleMessage));
    }

    /**
     * Register a receiver for a subscription. Note that we only allow a single receiver per subscription for push
     * messages. As push messages are regular HTTP calls under the hood, we can only "ack" or "nack" all of them in one go.
     * To avoid inconsistencies, multi-plexing the messages is left to the {@link MessageReceiver} implementation itself.
     *
     * @param subscriptionName The subscription name
     * @param receiver The message receiver
     * @return An api service to start and stop the listener
     */
    public SubscriberInterface registerListener(ProjectSubscriptionName subscriptionName, MessageReceiver receiver) {
        if (!enabled) {
            throw new IllegalStateException("Cannot register a push subscription when push is not enabled");
        }

        var service = new PushSubscriber(receiver);
        var previous = messageReceivers.putIfAbsent(subscriptionName, service);
        if (previous != null) {
            throw new IllegalStateException("Cannot register a new push subscription when push is already registered");
        }
        return service;
    }

    /**
     * Handler method for pubsub messages. This method is public in case you want to setup your own handler and augment
     * that with additional logic. This handler method will be used in case the endpoint-path is configured.
     *
     * @param rc The routing context
     */
    public void handleMessage(RoutingContext rc) {
        var message = extractMessage(rc);
        dispatchToListeners(rc, message);
    }

    private PubSubMessageJson extractMessage(RoutingContext rc) {
        return rc.body().asPojo(PubSubMessageJson.class);
    }

    private void dispatchToListeners(RoutingContext rc, PubSubMessageJson message) {
        var subscriptionName = ProjectSubscriptionName.parse(message.subscription);
        LOGGER.debug("Received push message from subscription " + subscriptionName);

        var receiver = messageReceivers.get(subscriptionName);
        if (receiver != null) {
            try {
                receiver.receiveMessage(message.toPubsub(), new AckReplyConsumer() {
                    @Override
                    public void ack() {
                        rc.response().setStatusCode(200).end();
                    }

                    @Override
                    public void nack() {
                        LOGGER.warn("Message receiver indicated the message was not processed correctly. Message " +
                                "will be failed for subscription " + subscriptionName);
                        rc.fail(500);
                    }
                });
            } catch (IOException e) {
                LOGGER.error("Failed to process the pubsub message", e);
                rc.fail(400);
            }
        } else {
            LOGGER.debug("No receiver registered for subscription " + subscriptionName + ". The meessage will be dropped");
            rc.end();
        }
    }

    private record PubSubMessageJson(
            MessageJson message,
            String subscription) {

        PubsubMessage toPubsub() throws IOException {
            var builder = PubsubMessage.newBuilder();
            builder.setMessageId(message().messageId);
            builder.setData(toByteString(decode(message.data)));

            if (message.attributes != null) {
                builder.putAllAttributes(message.attributes);
            }

            if (message.publishTime != null) {
                builder.setPublishTime(Timestamp.parseFrom(toByteString(message.publishTime)));
            }

            return builder.build();
        }

        private String decode(String data) {
            return new String(Base64.getDecoder().decode(data));
        }

        private ByteString toByteString(String data) {
            return ByteString.copyFromUtf8(data);
        }

    }

    private record MessageJson(
            String messageId,
            String publishTime,
            String data,
            Map<String, String> attributes) {

    }

    private static class PushSubscriber extends AbstractApiService implements MessageReceiver, SubscriberInterface {
        private final MessageReceiver receiver;
        private boolean started;

        private PushSubscriber(MessageReceiver receiver) {
            this.receiver = receiver;
            this.started = true;
        }

        @Override
        protected void doStart() {
            this.started = true;
            this.notifyStarted();
        }

        @Override
        protected void doStop() {
            this.started = false;
            this.notifyStopped();
        }

        @Override
        public void receiveMessage(PubsubMessage pubsubMessage, AckReplyConsumer ackReplyConsumer) {
            if (started) {
                receiver.receiveMessage(pubsubMessage, ackReplyConsumer);
            } else {
                ackReplyConsumer.nack();
            }
        }
    }
}
