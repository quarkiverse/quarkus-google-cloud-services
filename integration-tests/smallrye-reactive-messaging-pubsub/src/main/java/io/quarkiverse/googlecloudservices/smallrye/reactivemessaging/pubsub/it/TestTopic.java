package io.quarkiverse.googlecloudservices.smallrye.reactivemessaging.pubsub.it;

import java.util.Base64;

import org.eclipse.microprofile.reactive.messaging.Incoming;

public class TestTopic {
    @Incoming("my-topic")
    public void test(String str) {
        System.out.println("Receive => " + new String(Base64.getDecoder().decode(str)));
    }
}
