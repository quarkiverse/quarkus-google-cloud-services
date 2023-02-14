package io.quarkiverse.googlecloudservices.smallrye.reactivemessaging.pubsub.deployement;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class SmallryeReactiveMessagingPubsubSteps {
    private static final String FEATURE = "smallrye-reactive-messaging-pubsub";

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }
}
