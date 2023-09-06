package io.quarkiverse.googlecloudservices.secretmanager.runtime.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;

import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.ShutdownEvent;
import io.smallrye.config.SmallRyeConfig;

/**
 * The {@link SecretManagerConfigSource} keeps a reference to {@link SecretManagerServiceClient} that should be closed
 * on shutdown. Unfortunately the Config API does not provide a way to clean up resource, so this is probably the
 * easiest way on how to do it.
 * <p>
 * This will only work the current {@link SmallRyeConfig} registered in the current Classloader. If the application
 * registers {@link SmallRyeConfig} instances in another Classloader, or replaces the initial instance by another, the
 * close of {@link SecretManagerServiceClient} has to be handled by the application.
 */
@ApplicationScoped
@Unremovable
public class SecretManagerClientDestroyer {
    void onStop(@Observes ShutdownEvent ev) {
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        for (ConfigSource configSource : config.getConfigSources(SecretManagerConfigSource.class)) {
            SecretManagerConfigSource secretManagerConfigSource = (SecretManagerConfigSource) configSource;
            secretManagerConfigSource.closeClient();
        }
    }
}
