package io.quarkiverse.googlecloudservices.pubsub.push;

import java.io.IOException;
import java.text.ParseException;
import java.util.Base64;
import java.util.Map;

import com.google.protobuf.ByteString;
import com.google.protobuf.util.Timestamps;
import com.google.pubsub.v1.PubsubMessage;

/**
 * Represents the JSON form of the pubsub messages received via the HTTP endpoint
 *
 * @param message The message
 * @param subscription The subscription
 */
public record PubSubMessageJson(
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

    public record MessageJson(
            String messageId,
            String publishTime,
            String data,
            Map<String, String> attributes) {

    }
}
