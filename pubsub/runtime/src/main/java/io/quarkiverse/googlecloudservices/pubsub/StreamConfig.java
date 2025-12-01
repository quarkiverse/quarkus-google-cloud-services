package io.quarkiverse.googlecloudservices.pubsub;

/**
 * @param parallelStreamCount
 *        Number of concurrent streams to use for pull subscriptions.
 * @param streamConcurrency
 *        Number of concurrent messages to process per stream.
 */
public record StreamConfig(int parallelStreamCount, int streamConcurrency) {
}