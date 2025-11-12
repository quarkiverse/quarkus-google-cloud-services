package io.quarkiverse.googlecloudservices.pubsub;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.InstantiatingExecutorProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.SubscriberInterface;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.pubsub.v1.ProjectName;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.SubscriptionName;
import com.google.pubsub.v1.Topic;
import com.google.pubsub.v1.TopicName;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.quarkiverse.googlecloudservices.common.GcpConfigHolder;
import io.quarkiverse.googlecloudservices.pubsub.PubSubPullConfiguration.PullConfiguration;
import io.quarkiverse.googlecloudservices.pubsub.push.PubSubPushBuildTimeConfig;
import io.quarkiverse.googlecloudservices.pubsub.push.PubSubPushManager;

@ApplicationScoped
public class QuarkusPubSub {
    @Inject
    Instance<CredentialsProvider> credentialsProvider;

    @Inject
    GcpConfigHolder gcpConfigHolder;

    @Inject
    PubSubConfiguration pubSubConfiguration;

    @Inject
    Instance<PubSubPushManager> pushManager;

    @Inject
    PubSubPushBuildTimeConfig pushConfig;

    @Inject
    PubSubPullConfiguration pullConfig;

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

    /**
     * Creates a PubSub Subscriber using the configured project ID.
     */
    public SubscriberInterface subscriber(String subscription, MessageReceiver receiver) {
        return subscriber(subscription, gcpConfigHolder.getBootstrapConfig().projectId().orElseThrow(), receiver);
    }

    /**
     * Creates a PubSub Subscriber using the specified project ID.
     */
    public SubscriberInterface subscriber(String subscription, String projectId, MessageReceiver receiver) {
        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(projectId, subscription);
        if (pushConfig.enabled()) {
            return pushSubscriber(subscriptionName, receiver);
        } else {
            return pullSubscriber(subscriptionName, receiver, pullConfig.toPullConfiguration());
        }
    }

    /**
     * Creates a PubSub pull Subscriber using the specified project ID and pull configuration
     */
    public SubscriberInterface pullSubscriber(String subscription, String projectId, MessageReceiver receiver,
            PullConfiguration pullConfiguration) {
        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(projectId, subscription);
        return pullSubscriber(subscriptionName, receiver,
                pullConfiguration == null ? pullConfig.toPullConfiguration() : pullConfiguration);
    }

    private Subscriber pullSubscriber(ProjectSubscriptionName subscriptionName, MessageReceiver receiver,
            PullConfiguration pullConfiguration) {
        ExecutorProvider executorProvider = InstantiatingExecutorProvider.newBuilder()
                .setExecutorThreadCount(pullConfiguration.streamConcurrency()).build();
        var builder = Subscriber.newBuilder(subscriptionName, receiver)
                .setParallelPullCount(pullConfiguration.parallelStreamCount())
                .setExecutorProvider(executorProvider)
                .setCredentialsProvider(credentialsProvider());
        channelProvider.ifPresent(builder::setChannelProvider);
        return builder.build();
    }

    private SubscriberInterface pushSubscriber(ProjectSubscriptionName subscriptionName, MessageReceiver receiver) {
        return pushManager.get().registerListener(subscriptionName, receiver);
    }

    /**
     * Creates a PubSub Publisher using the configured project ID.
     */
    public Publisher publisher(String topic) throws IOException {
        return publisher(topic, (Consumer<Publisher.Builder>) null);
    }

    /**
     * Creates a PubSub Publisher using the configured project ID. The customizer can be used to change additional
     * settings on the builder
     */
    public Publisher publisher(String topic, Consumer<Publisher.Builder> customizer) throws IOException {
        return publisher(topic, gcpConfigHolder.getBootstrapConfig().projectId().orElseThrow(), customizer);
    }

    /**
     * Creates a PubSub Publisher using the specified project ID.
     */
    public Publisher publisher(String topic, String projectId) throws IOException {
        return publisher(topic, projectId, null);
    }

    /**
     * Creates a PubSub Publisher using the specified project ID.The customizer can be used to change additional
     * settings on the builder
     */
    public Publisher publisher(String topic, String projectId, Consumer<Publisher.Builder> customizer) throws IOException {
        TopicName topicName = TopicName.of(projectId, topic);
        var builder = Publisher.newBuilder(topicName)
                .setCredentialsProvider(credentialsProvider());
        channelProvider.ifPresent(builder::setChannelProvider);

        if (customizer != null) {
            customizer.accept(builder);
        }

        return builder.build();
    }

    /**
     * Creates a PubSub SubscriptionAdminSettings using the configured project ID.
     */
    public SubscriptionAdminSettings subscriptionAdminSettings() throws IOException {
        var builder = SubscriptionAdminSettings.newBuilder()
                .setCredentialsProvider(credentialsProvider());
        channelProvider.ifPresent(builder::setTransportChannelProvider);
        return builder.build();
    }

    /**
     * Creates a PubSub TopicAdminSettings using the configured project ID.
     */
    public TopicAdminSettings topicAdminSettings() throws IOException {
        var builder = TopicAdminSettings.newBuilder()
                .setCredentialsProvider(credentialsProvider());
        channelProvider.ifPresent(builder::setTransportChannelProvider);
        return builder.build();
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

    private CredentialsProvider credentialsProvider() {
        if (pubSubConfiguration.emulatorHost().isPresent() && pubSubConfiguration.useEmulatorCredentials()) {
            return new NoCredentialsProvider();
        } else {
            return credentialsProvider.get();
        }
    }
}
