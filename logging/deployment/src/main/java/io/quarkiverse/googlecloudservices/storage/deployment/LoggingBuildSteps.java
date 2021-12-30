package io.quarkiverse.googlecloudservices.storage.deployment;

import com.google.auth.oauth2.GoogleCredentials;

import co.elastic.logging.AdditionalField;
import co.elastic.logging.EcsJsonSerializer;
import co.elastic.logging.JsonUtils;
import io.quarkiverse.googlecloudservices.storage.runtime.LoggingConfiguration;
import io.quarkiverse.googlecloudservices.storage.runtime.LoggingHandlerFactory;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LogHandlerBuildItem;

public class LoggingBuildSteps {

    private static final String FEATURE = "google-cloud-logging";

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public UnremovableBeanBuildItem credentials() {
        return UnremovableBeanBuildItem.beanTypes(GoogleCredentials.class);
    }

    @BuildStep
    public UnremovableBeanBuildItem escSerializer() {
        return UnremovableBeanBuildItem.beanTypes(AdditionalField.class, EcsJsonSerializer.class, JsonUtils.class);
    }

    @BuildStep
    public UnremovableBeanBuildItem escTimestampSerializer() {
        return UnremovableBeanBuildItem.beanClassNames("co.elastic.logging.TimestampSerializer");
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public LogHandlerBuildItem handler(LoggingConfiguration config, LoggingHandlerFactory factory) {
        return new LogHandlerBuildItem(factory.create(config));
    }
}
