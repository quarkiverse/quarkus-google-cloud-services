package io.quarkiverse.googlecloudservices.it;

import java.io.IOException;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.google.api.core.ApiService;
import com.google.cloud.pubsub.v1.MessageReceiver;

import io.quarkiverse.googlecloudservices.pubsub.QuarkusPubSub;
import io.quarkus.runtime.StartupEvent;

@Path("/pubsub-push")
public class PubSubPushResource {
    private static final Logger LOG = Logger.getLogger(PubSubResource.class);

    @Inject
    QuarkusPubSub pubSub;

    @ConfigProperty(name = "quarkus.google.cloud.pubsub.push.enabled")
    boolean pushEnabled;

    private ApiService subscriber;
    private String lastMessage = "NO MESSAGE YET";

    public void init(@Observes StartupEvent ev) throws IOException {
        if (!pushEnabled) {
            return;
        }

        // subscribe to PubSub
        MessageReceiver receiver = (message, consumer) -> {
            this.lastMessage = message.getData().toStringUtf8();
            LOG.infov("Got message {0}", this.lastMessage);
            consumer.ack();
        };
        subscriber = pubSub.subscriber("test-push-subscription", receiver);
        subscriber.startAsync().awaitRunning();
    }

    @PreDestroy
    void destroy() {
        if (subscriber != null) {
            subscriber.stopAsync();
        }
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String verifyMessage() {
        return this.lastMessage;
    }
}
