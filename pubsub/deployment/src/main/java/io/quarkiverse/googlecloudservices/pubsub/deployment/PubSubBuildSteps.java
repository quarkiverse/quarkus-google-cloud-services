package io.quarkiverse.googlecloudservices.pubsub.deployment;

import io.quarkiverse.googlecloudservices.pubsub.*;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class PubSubBuildSteps {
    protected static final String FEATURE = "google-cloud-pubsub";

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public AdditionalBeanBuildItem producer() {
        return new AdditionalBeanBuildItem(QuarkusPubSub.class, PubSubProducer.class);
    }

    @BuildStep
    public void setupPush(BuildProducer<AdditionalBeanBuildItem> additionalBeans, PubSubBuildTimeConfig config) {
        if (!config.push().enabled()) {
            return;
        }

        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder().setUnremovable();

        builder.addBeanClasses(GooglePubSubAuthenticationFilter.class, PubSubPushManager.class, GoogleTokenVerifier.class);
        additionalBeans.produce(builder.build());
    }

}
