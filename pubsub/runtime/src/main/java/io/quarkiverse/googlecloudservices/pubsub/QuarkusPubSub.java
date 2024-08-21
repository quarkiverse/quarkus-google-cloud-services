package io.quarkiverse.googlecloudservices.pubsub;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.StreamSupport;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.*;
import com.google.pubsub.v1.*;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.quarkiverse.googlecloudservices.common.GcpConfigHolder;

@ApplicationScoped
public class QuarkusPubSub {
    @Inject
    CredentialsProvider credentialsProvider;

    @Inject
    GcpConfigHolder gcpConfigHolder;

    @Inject
    PubSubConfiguration pubSubConfiguration;

    private Optional<TransportChannelProvider> channelProvider;

    @PostConstruct
    void init() {
        if (pubSubConfiguration.emulatorHost().isPresent()) {
            ManagedChannel channel = ManagedChannelBuilder.forTarget(pubSubConfiguration.emulatorHost().get()).usePlaintext()
                    .build();
            channelProvider = Optional.of(FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel)));
        } else {
            channelProvider = Optional.empty();
        }
    }

    @PreDestroy
    void destroy() throws Exception {
        if (channelProvider.isPresent()) {
            channelProvider.get().getTransportChannel().close();
        }
    }

    /**
     * Creates a PubSub Subscriber using the configured project ID.
     */
    public Subscriber subscriber(String subscription, MessageReceiver receiver) {
        return subscriber(subscription, gcpConfigHolder.getBootstrapConfig().projectId().orElseThrow(), receiver);
    }

    /**
     * Creates a PubSub Subscriber using the specified project ID.
     */
    public Subscriber subscriber(String subscription, String projectId, MessageReceiver receiver) {
        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(projectId, subscription);
        var builder = Subscriber.newBuilder(subscriptionName, receiver)
                .setCredentialsProvider(credentialsProvider);
        channelProvider.ifPresent(builder::setChannelProvider);
        return builder.build();

    }

    /**
     * Creates a PubSub Publisher using the configured project ID.
     */
    public Publisher publisher(String topic) throws IOException {
        return publisher(topic, gcpConfigHolder.getBootstrapConfig().projectId().orElseThrow());
    }

    /**
     * Creates a PubSub Publisher using the specified project ID.
     */
    public Publisher publisher(String topic, String projectId) throws IOException {
        TopicName topicName = TopicName.of(projectId, topic);
        var builder = Publisher.newBuilder(topicName)
                .setCredentialsProvider(credentialsProvider);
        channelProvider.ifPresent(builder::setChannelProvider);
        return builder.build();
    }

    /**
     * Creates a PubSub SubscriptionAdminSettings using the configured project ID.
     */
    public SubscriptionAdminSettings subscriptionAdminSettings() throws IOException {
        var builder = SubscriptionAdminSettings.newBuilder()
                .setCredentialsProvider(credentialsProvider);
        channelProvider.ifPresent(builder::setTransportChannelProvider);
        return builder.build();
    }

    /**
     * Makes the subscription admin client available as CDI bean
     */
    @Produces
    public SubscriptionAdminClient subscriptionAdminClient() throws IOException {
        return SubscriptionAdminClient.create(subscriptionAdminSettings());
    }

    /**
     * CDI Dispose method for {@link #subscriptionAdminClient()}. Shouldn't be called directly
     */
    public void shutdownSubscriptionAdminClient(@Disposes SubscriptionAdminClient subscriptionAdminClient) {
        subscriptionAdminClient.close();
    }

    /**
     * Creates a PubSub TopicAdminSettings using the configured project ID.
     */
    public TopicAdminSettings topicAdminSettings() throws IOException {
        var builder = TopicAdminSettings.newBuilder()
                .setCredentialsProvider(credentialsProvider);
        channelProvider.ifPresent(builder::setTransportChannelProvider);
        return builder.build();
    }

    /**
     * Makes the topic admin client available as a CDI bean
     */
    @Produces
    public TopicAdminClient topicAdminClient() throws IOException {
        return TopicAdminClient.create(topicAdminSettings());
    }

    /**
     * CDI Dispose method for {@link #topicAdminClient()}. Shouldn't be called directly
     */
    public void shutdownTopicAdminClient(@Disposes TopicAdminClient topicAdminClient) {
        topicAdminClient.close();
    }

    /**
     * Creates a PubSub Topic if not already exist, using the configured project ID.
     */
    public Topic createTopic(String topic) throws IOException {
        TopicAdminSettings topicAdminSettings = topicAdminSettings();
        TopicName topicName = TopicName.of(gcpConfigHolder.getBootstrapConfig().projectId().orElseThrow(), topic);

        try (TopicAdminClient topicAdminClient = TopicAdminClient.create(topicAdminSettings)) {
            Iterable<Topic> topics = topicAdminClient
                    .listTopics(ProjectName.of(gcpConfigHolder.getBootstrapConfig().projectId().orElseThrow()))
                    .iterateAll();
            Optional<Topic> existing = StreamSupport.stream(topics.spliterator(), false)
                    .filter(top -> top.getName().equals(topicName.toString()))
                    .findFirst();
            return existing.orElseGet(() -> topicAdminClient.createTopic(topicName.toString()));
        }
    }

    /**
     * Creates a PubSub Subscription if not already exist, using the configured project ID.
     */
    public Subscription createSubscription(String topic, String subscription) throws IOException {
        SubscriptionName subscriptionName = SubscriptionName.of(gcpConfigHolder.getBootstrapConfig().projectId().orElseThrow(),
                subscription);
        TopicName topicName = TopicName.of(gcpConfigHolder.getBootstrapConfig().projectId().orElseThrow(), topic);
        SubscriptionAdminSettings subscriptionAdminSettings = subscriptionAdminSettings();

        try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create(subscriptionAdminSettings)) {
            Iterable<Subscription> subscriptions = subscriptionAdminClient
                    .listSubscriptions(ProjectName.of(gcpConfigHolder.getBootstrapConfig().projectId().orElseThrow()))
                    .iterateAll();
            Optional<Subscription> existing = StreamSupport.stream(subscriptions.spliterator(), false)
                    .filter(sub -> sub.getName().equals(subscriptionName.toString()))
                    .findFirst();
            return existing.orElseGet(() -> subscriptionAdminClient.createSubscription(subscriptionName, topicName,
                    PushConfig.getDefaultInstance(), 0));
        }
    }
}
