package io.quarkiverse.googlecloudservices.logging.deployment;

import java.util.ArrayList;
import java.util.List;

import io.quarkiverse.googlecloudservices.logging.runtime.LoggingConfiguration;
import io.quarkiverse.googlecloudservices.logging.runtime.TraceInfoExtractor;
import io.quarkiverse.googlecloudservices.logging.runtime.cdi.LoggingProducer;
import io.quarkiverse.googlecloudservices.logging.runtime.recorder.LoggingHandlerFactory;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LogHandlerBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

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
    public UnremovableBeanBuildItem helperClasses() {
        return UnremovableBeanBuildItem.beanTypes(
                TraceInfoExtractor.class,
                LoggingConfiguration.class);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public LogHandlerBuildItem handler(LoggingConfiguration config, LoggingHandlerFactory factory) {
        return new LogHandlerBuildItem(factory.create(config));
    }

    @BuildStep
    public List<ReflectiveClassBuildItem> registerReflectiveClasses() {
        List<ReflectiveClassBuildItem> items = new ArrayList<>();
        // Required for native builds as LoggingHandler uses Gson to serialize severity values
        items.add(ReflectiveClassBuildItem.builder("com.google.cloud.logging.Severity")
                .fields(true)
                .build());
        return items;
    }
}
