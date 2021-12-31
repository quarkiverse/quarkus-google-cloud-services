package io.quarkiverse.googlecloudservices.logging.deployment;

import com.google.auth.oauth2.GoogleCredentials;

import io.quarkiverse.googlecloudservices.logging.runtime.JsonFormatter;
import io.quarkiverse.googlecloudservices.logging.runtime.LoggingConfiguration;
import io.quarkiverse.googlecloudservices.logging.runtime.ecs.EscJsonFormatter;
import io.quarkiverse.googlecloudservices.logging.runtime.recorder.LoggingHandlerFactory;
import io.quarkiverse.googlecloudservices.logging.runtime.util.LevelTransformer;
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
    public UnremovableBeanBuildItem helperClasses() {
        return UnremovableBeanBuildItem.beanTypes(EscJsonFormatter.class, LevelTransformer.class, JsonFormatter.class);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public LogHandlerBuildItem handler(LoggingConfiguration config, LoggingHandlerFactory factory) {
        return new LogHandlerBuildItem(factory.create(config));
    }
}
