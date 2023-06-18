package io.quarkiverse.googlecloudservices.logging.deployment;

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
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;

public class LoggingBuildSteps {

    private static final String FEATURE = "google-cloud-logging";
    private static final String QUARKUS_CONSOLE_LOGGING_CONFIG_KEY = "quarkus.log.console.enable";

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
    public RunTimeConfigurationDefaultBuildItem configurationDefaultBuildItem(LoggingConfiguration config) {
        boolean enableConsoleLogging = true;
        // We should use configuration only if the GCP logging extension is enabled
        if (config.enabled) {
            enableConsoleLogging = config.enableConsoleLogging;
        }
        return new RunTimeConfigurationDefaultBuildItem(QUARKUS_CONSOLE_LOGGING_CONFIG_KEY,
                Boolean.toString(enableConsoleLogging));
    }
}
