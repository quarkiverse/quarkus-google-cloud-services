package io.quarkiverse.googlecloudservices.pubsub;

import java.io.IOException;

import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminClient;

/**
 * Producer class for PubSub beans.
 */
public class PubSubProducer {

    @Inject
    QuarkusPubSub quarkusPubSub;

    /**
     * Makes the subscription admin client available as CDI bean
     */
    @Produces
    public SubscriptionAdminClient subscriptionAdminClient() throws IOException {
        return SubscriptionAdminClient.create(quarkusPubSub.subscriptionAdminSettings());
    }

    /**
     * CDI Dispose method for {@link #subscriptionAdminClient()}. Shouldn't be called directly
     */
    public void shutdownSubscriptionAdminClient(@Disposes SubscriptionAdminClient subscriptionAdminClient) {
        subscriptionAdminClient.close();
    }

    /**
     * Makes the topic admin client available as a CDI bean
     */
    @Produces
    public TopicAdminClient topicAdminClient() throws IOException {
        return TopicAdminClient.create(quarkusPubSub.topicAdminSettings());
    }

    /**
     * CDI Dispose method for {@link #topicAdminClient()}. Shouldn't be called directly
     */
    public void shutdownTopicAdminClient(@Disposes TopicAdminClient topicAdminClient) {
        topicAdminClient.close();
    }

}
