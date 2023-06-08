package io.quarkiverse.googlecloudservices.it.pubsub;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.StreamSupport;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.*;
import com.google.pubsub.v1.*;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

@ApplicationScoped
public class TopicManager {
    @Inject
    CredentialsProvider credentialsProvider;

    @ConfigProperty(name = "quarkus.google.cloud.project-id")
    String projectId;

    @ConfigProperty(name = "pubsub.use-emulator", defaultValue = "false")
    boolean useEmulator;

    @ConfigProperty(name = "quarkus.google.cloud.pubsub.emulator-host")
    String emulatorHost;

    private TopicName topicName;
    private Optional<TransportChannelProvider> channelProvider;

    @PostConstruct
    void init() {
        this.topicName = TopicName.of(projectId, "test-topic");

        if (useEmulator) {
            ManagedChannel channel = ManagedChannelBuilder.forTarget(emulatorHost).usePlaintext().build();
            channelProvider = Optional.of(FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel)));
        } else {
            channelProvider = Optional.empty();
        }
    }

    public Subscriber initSubscriber(MessageReceiver receiver) throws IOException {
        ProjectSubscriptionName subscriptionName = initTopicAndSubscription();
        var builder = Subscriber.newBuilder(subscriptionName, receiver)
                .setCredentialsProvider(credentialsProvider);
        channelProvider.ifPresent(builder::setChannelProvider);
        return builder.build();
    }

    public Publisher initPublisher() throws IOException {
        var builder = Publisher.newBuilder(topicName)
                .setCredentialsProvider(credentialsProvider);
        channelProvider.ifPresent(builder::setChannelProvider);
        return builder.build();
    }

    private ProjectSubscriptionName initTopicAndSubscription() throws IOException {
        TopicAdminSettings topicAdminSettings = getTopicAdminSettings();
        try (TopicAdminClient topicAdminClient = TopicAdminClient.create(topicAdminSettings)) {
            Iterable<Topic> topics = topicAdminClient.listTopics(ProjectName.of(projectId)).iterateAll();
            Optional<Topic> existing = StreamSupport.stream(topics.spliterator(), false)
                    .filter(topic -> topic.getName().equals(topicName.toString()))
                    .findFirst();
            if (existing.isEmpty()) {
                topicAdminClient.createTopic(topicName.toString());
            }
        }

        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(projectId, "test-subscription");
        SubscriptionAdminSettings subscriptionAdminSettings = getSubscriptionAdminSettings();
        try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create(subscriptionAdminSettings)) {
            Iterable<Subscription> subscriptions = subscriptionAdminClient.listSubscriptions(ProjectName.of(projectId))
                    .iterateAll();
            Optional<Subscription> existing = StreamSupport.stream(subscriptions.spliterator(), false)
                    .filter(sub -> sub.getName().equals(subscriptionName.toString()))
                    .findFirst();
            if (existing.isEmpty()) {
                subscriptionAdminClient.createSubscription(subscriptionName, topicName, PushConfig.getDefaultInstance(), 0);
            }
        }
        return subscriptionName;
    }

    private SubscriptionAdminSettings getSubscriptionAdminSettings() throws IOException {
        var builder = SubscriptionAdminSettings.newBuilder()
                .setCredentialsProvider(credentialsProvider);
        channelProvider.ifPresent(builder::setTransportChannelProvider);
        return builder.build();
    }

    private TopicAdminSettings getTopicAdminSettings() throws IOException {
        var builder = TopicAdminSettings.newBuilder()
                .setCredentialsProvider(credentialsProvider);
        channelProvider.ifPresent(builder::setTransportChannelProvider);
        return builder.build();
    }
}
