package io.quarkiverse.googlecloudservices.pubsub.push;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.pubsub.v1.ProjectSubscriptionName;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.IOException;

/**
 * This class implements the generic dispatching logic of messages and translating between HTTP based push requests
 * and dispatching the messages. Using the {@link Response} interface, the interaction with the underlying transport
 * is handled. Providers of a transport need to implement the {@link Response} interface and use this class to
 * dispatch messages to the listeners.
 */
@ApplicationScoped
public class MessageDispatcher {
    private static final Logger LOGGER = Logger.getLogger(MessageDispatcher.class.getName());

    @Inject
    PubSubPushManager manager;

    /**
     * Adapter interface to translate the message handling action to the underlying transport mechanism
     */
    public interface Response extends AckReplyConsumer {

        void ioError();

        void discard();

    }

    /**
     * Handle the message and dispatch it to the registered listeners.
     *
     * @param message The message to dispatch
     * @param response The response adapter interface to translate to the underlying transport.
     */
    public void dispatchMessageToListener(PubSubMessageJson message, Response response) {
        var subscriptionName = ProjectSubscriptionName.parse(message.subscription());
        LOGGER.debug(
                "Received push message from subscription " + subscriptionName + " send at " + message.message().publishTime());

        var receiver = getMessageReceiver(subscriptionName);
        if (receiver != null) {
            try {
                receiver.receiveMessage(message.toPubsub(), new AckReplyConsumer() {
                    @Override
                    public void ack() {
                        LOGGER.trace("Message receiver acknowledged the message " + message.message().messageId());
                        response.ack();
                    }

                    @Override
                    public void nack() {
                        LOGGER.warn("Message receiver indicated the message was not processed correctly. Message " +
                                message.message().messageId() + " will be failed for subscription " + subscriptionName);
                        response.nack();
                    }
                });
            } catch (IOException e) {
                LOGGER.error("Failed to process the pubsub message", e);
                response.ioError();
            }
        } else {
            LOGGER.debug("No receiver registered for subscription " + subscriptionName + ". The meessage will be dropped");
            response.discard();
        }
    }

    private MessageReceiver getMessageReceiver(ProjectSubscriptionName subscriptionName) {
        return manager.getReceiver(subscriptionName);
    }

}
