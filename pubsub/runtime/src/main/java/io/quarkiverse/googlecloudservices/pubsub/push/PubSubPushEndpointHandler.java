package io.quarkiverse.googlecloudservices.pubsub.push;

import java.io.IOException;
import java.text.ParseException;
import java.util.Base64;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.protobuf.ByteString;
import com.google.protobuf.util.Timestamps;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;

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
        var subscriptionName = ProjectSubscriptionName.parse(message.subscription);
        LOGGER.debug("Received push message from subscription " + subscriptionName + " send at " + message.message.publishTime);

        var receiver = getMessageReceiver(subscriptionName);
        if (receiver != null) {
            try {
                receiver.receiveMessage(message.toPubsub(), new AckReplyConsumer() {
                    @Override
                    public void ack() {
                        LOGGER.trace("Message receiver acknowledged the message " + message.message().messageId);
                        rc.response().setStatusCode(200).end();
                    }

                    @Override
                    public void nack() {
                        LOGGER.warn("Message receiver indicated the message was not processed correctly. Message " +
                                message.message().messageId + " will be failed for subscription " + subscriptionName);
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

    /**
     * Represents the JSON form of the pubsub messages received via the HTTP endpoint
     *
     * @param message The message
     * @param subscription The subscription
     */
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
                try {
                    builder.setPublishTime(Timestamps.parse(message.publishTime()));
                } catch (ParseException e) {
                    throw new IOException("Failed to parse publish time " + message.publishTime(), e);
                }
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
}
