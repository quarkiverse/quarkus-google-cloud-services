package io.quarkiverse.googlecloudservices.pubsub.push;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler class for pubsub push messages.
 */
@ApplicationScoped
public class PubSubPushEndpointHandler implements Handler<RoutingContext> {
    @Inject
    MessageDispatcher dispatcher;

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
        dispatcher.dispatchMessageToListener(message, new MessageDispatcher.Response() {
            @Override
            public void ioError() {
                rc.fail(400);
            }

            @Override
            public void discard() {
                rc.end();
            }

            @Override
            public void ack() {
                rc.response().setStatusCode(200).end();
            }

            @Override
            public void nack() {
                rc.fail(500);
            }
        });
    }

}
