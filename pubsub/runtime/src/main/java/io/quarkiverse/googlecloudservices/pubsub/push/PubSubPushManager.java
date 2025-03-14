package io.quarkiverse.googlecloudservices.pubsub.push;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.google.api.core.AbstractApiService;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.SubscriberInterface;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;

/**
 * This class manages pub-sub push subscriptions (see <a href="https://cloud.google.com/pubsub/docs/push">here</a>).
 * <p>
 * It provides a way to subscribe to messages (via {@link io.quarkiverse.googlecloudservices.pubsub.QuarkusPubSub}
 * comparable to regular pull subscriptions. Also handlers are implemented to handle the incoming HTTP connections.
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
     * Return the receiver for a subscription name
     *
     * @param subscriptionName The subscription name
     * @return The receiver or null if not found
     */
    public MessageReceiver getReceiver(ProjectSubscriptionName subscriptionName) {
        return messageReceivers.get(subscriptionName);
    }

    private static class PushSubscriber extends AbstractApiService implements MessageReceiver, SubscriberInterface {
        private final MessageReceiver receiver;
        private final AtomicBoolean started;

        private PushSubscriber(MessageReceiver receiver) {
            this.receiver = receiver;
            this.started = new AtomicBoolean(true);
        }

        @Override
        protected void doStart() {
            this.started.set(true);
            this.notifyStarted();
        }

        @Override
        protected void doStop() {
            this.started.set(false);
            this.notifyStopped();
        }

        @Override
        public void receiveMessage(PubsubMessage pubsubMessage, AckReplyConsumer ackReplyConsumer) {
            LOGGER.debug("Received a push message " + pubsubMessage.getMessageId());
            if (started.get()) {
                receiver.receiveMessage(pubsubMessage, ackReplyConsumer);
            } else {
                LOGGER.debug(
                        "Push message " + pubsubMessage.getMessageId() + " not has been delivered. Receiver was not started");
                ackReplyConsumer.nack();
            }
        }
    }
}
