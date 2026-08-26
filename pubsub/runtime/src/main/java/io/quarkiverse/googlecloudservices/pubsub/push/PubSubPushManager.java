package io.quarkiverse.googlecloudservices.pubsub.push;

import static io.quarkiverse.googlecloudservices.pubsub.push.GooglePubSubAuthenticationHandler.VERIFICATION_TOKEN_NAME;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.google.api.core.AbstractApiService;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.SubscriberInterface;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;

import io.quarkus.vertx.http.HttpServer;

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

    @ConfigProperty(name = "quarkus.google.cloud.pubsub.push.endpoint-path")
    Optional<String> endpointPath;

    @ConfigProperty(name = "quarkus.google.cloud.pubsub.push.verification-token")
    Optional<String> verificationToken;

    @Inject
    Instance<HttpServer> httpServer;

    /**
     * Get the endpoint URL for the push subscription. This can be used for testing to configure the push endpoints
     * for the subscriptions. Note that this method will throw an exception if push is not enabled or if the endpoint
     * path is not configured.
     * <p/>
     * The host parameter is typically <code>localhost</code>, however when e.g. running in an integration test setup,
     * the container much be made available through TestContainer#expostHostPort(), in which case the host paramter
     * is set to "host.testcontainers.internal".
     * <p/>
     * This method will use the configured HTTP server to determine the port and scheme (http or https) to use for the
     * endpoint URL.
     * <p/>
     * Note that this method will throw an exception if the HTTP server is not available or if the port cannot be
     * determined.
     * <p/>
     * This method will also append the verification token as a query parameter if it is configured.
     *
     * @param host The host name to use for the endpoint URL. This is typically the public hostname of the application.
     * @return The endpoint URL for the push subscription
     */
    public URI getEndpointUrl(String host) {
        if (!enabled) {
            throw new IllegalStateException("Cannot get the endpoint URL when push is not enabled");
        }
        if (endpointPath.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot get the endpoint URL when push is enabled but no endpoint path is configured");
        }

        var httpServerInstance = httpServer.get();

        var port = httpServerInstance.getPort();
        var scheme = "http";
        if (port == -1) {
            port = httpServerInstance.getSecurePort();
            scheme = "https";
            if (port == -1) {
                throw new IllegalStateException(
                        "Cannot get the endpoint URL when push is enabled but no HTTP port can be resolved");
            }
        }
        try {
            return new URI(
                    scheme,
                    null,
                    host,
                    port,
                    endpointPath.get(),
                    verificationToken.map(token -> VERIFICATION_TOKEN_NAME + "=" + token).orElse(null),
                    null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Cannot construct endpoint URL", e);
        }
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
