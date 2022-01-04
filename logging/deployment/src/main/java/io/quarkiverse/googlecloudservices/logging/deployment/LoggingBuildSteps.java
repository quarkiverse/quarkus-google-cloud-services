package io.quarkiverse.googlecloudservices.logging.deployment;

import com.google.auth.oauth2.GoogleCredentials;

import io.quarkiverse.googlecloudservices.logging.runtime.JsonFormatter;
import io.quarkiverse.googlecloudservices.logging.runtime.LabelExtractor;
import io.quarkiverse.googlecloudservices.logging.runtime.LoggingConfiguration;
import io.quarkiverse.googlecloudservices.logging.runtime.TraceInfo;
import io.quarkiverse.googlecloudservices.logging.runtime.TraceInfoExtractor;
import io.quarkiverse.googlecloudservices.logging.runtime.cdi.LoggingProducer;
import io.quarkiverse.googlecloudservices.logging.runtime.cdi.WriteOptionsProducer;
import io.quarkiverse.googlecloudservices.logging.runtime.ecs.EscJsonFormat;
import io.quarkiverse.googlecloudservices.logging.runtime.format.JsonHandler;
import io.quarkiverse.googlecloudservices.logging.runtime.format.TextHandler;
import io.quarkiverse.googlecloudservices.logging.runtime.recorder.LoggingHandlerFactory;
import io.quarkiverse.googlecloudservices.logging.runtime.util.LevelTransformer;
import io.quarkiverse.googlecloudservices.logging.runtime.util.SimpleFormatter;
import io.quarkiverse.googlecloudservices.logging.runtime.util.StackTraceArrayRenderer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
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
    public AdditionalBeanBuildItem logging() {
        return new AdditionalBeanBuildItem(LoggingProducer.class);
    }

    @BuildStep
    public AdditionalBeanBuildItem writeOptions() {
        return new AdditionalBeanBuildItem(WriteOptionsProducer.class);
    }

    @BuildStep
    public UnremovableBeanBuildItem credentials() {
        return UnremovableBeanBuildItem.beanTypes(GoogleCredentials.class);
    }

    @BuildStep
    public UnremovableBeanBuildItem helperClasses() {
        return UnremovableBeanBuildItem.beanTypes(
                EscJsonFormat.class,
                LevelTransformer.class,
                JsonFormatter.class,
                LabelExtractor.class,
                LoggingConfiguration.class,
                TraceInfoExtractor.class,
                TraceInfo.class,
                JsonHandler.class,
                TextHandler.class,
                SimpleFormatter.class,
                StackTraceArrayRenderer.class);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public LogHandlerBuildItem handler(LoggingConfiguration config, LoggingHandlerFactory factory) {
        return new LogHandlerBuildItem(factory.create(config));
    }
}
