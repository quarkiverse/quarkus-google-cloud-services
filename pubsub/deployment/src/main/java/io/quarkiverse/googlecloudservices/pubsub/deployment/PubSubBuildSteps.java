package io.quarkiverse.googlecloudservices.pubsub.deployment;

import io.quarkiverse.googlecloudservices.pubsub.*;
import io.quarkiverse.googlecloudservices.pubsub.push.*;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;

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
    @Record(ExecutionTime.RUNTIME_INIT)
    public void setupPush(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<RouteBuildItem> additionalRoutes,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            PubSubPushRecorder recorder,
            PubSubBuildTimeConfig config) {
        if (!config.push().enabled()) {
            return;
        }

        additionalBeans.produce(AdditionalBeanBuildItem
                .builder()
                .setUnremovable()
                .addBeanClasses(
                        GooglePubSubAuthenticationHandler.class,
                        PubSubPushManager.class,
                        GoogleTokenVerifier.class,
                        PubSubPushEndpointHandler.class)
                .build());

        config.push().endpointPath().ifPresent(endpoint -> additionalRoutes.produce(nonApplicationRootPathBuildItem
                .routeBuilder()
                .routeFunction(endpoint, recorder.routeFunction())
                .build()));

    }

}
