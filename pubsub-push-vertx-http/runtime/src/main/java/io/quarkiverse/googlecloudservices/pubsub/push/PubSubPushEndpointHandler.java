package io.quarkiverse.googlecloudservices.pubsub.push;

import java.io.IOException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.pubsub.v1.ProjectSubscriptionName;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler class for pubsub push messages.
 */
@ApplicationScoped
public class PubSubPushEndpointHandler implements Handler<RoutingContext> {
    private static final Logger LOGGER = Logger.getLogger(PubSubPushEndpointHandler.class.getName());

    @Inject
    PubSubPushManager manager;

    /**
     * Handler method for pubsub messages. This method is public in case you want to setup your own handler and augment
     * that with additional logic. This handler method will be used in case the endpoint-path is configured.
     *
     * @param rc The routing context
     */
    @Override
    public void handle(RoutingContext rc) {
        var message = extractMessage(rc);
        dispatchToListeners(rc, message);
    }

    private PubSubMessageJson extractMessage(RoutingContext rc) {
        return rc.body().asPojo(PubSubMessageJson.class);
    }

    private void dispatchToListeners(RoutingContext rc, PubSubMessageJson message) {
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
                        rc.response().setStatusCode(200).end();
                    }

                    @Override
                    public void nack() {
                        LOGGER.warn("Message receiver indicated the message was not processed correctly. Message " +
                                message.message().messageId() + " will be failed for subscription " + subscriptionName);
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

    private MessageReceiver getMessageReceiver(ProjectSubscriptionName subscriptionName) {
        return manager.getReceiver(subscriptionName);
    }

}
