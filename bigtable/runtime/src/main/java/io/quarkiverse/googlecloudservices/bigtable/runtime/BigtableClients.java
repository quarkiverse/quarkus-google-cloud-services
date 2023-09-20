package io.quarkiverse.googlecloudservices.bigtable.runtime;

import org.jboss.logging.Logger;

import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.BigtableDataSettings;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.runtime.LaunchMode;

public class BigtableClients {

    private static final Logger LOGGER = Logger.getLogger(BigtableClients.class.getName());

    public static BigtableDataClient createClient(String name) throws Exception {
        ArcContainer container = Arc.container();

        InstanceHandle<BigtableConfigProvider> configProvider = container.instance(BigtableConfigProvider.class);
        if (!configProvider.isAvailable()) {
            throw new Exception("BigtableConfigProvider not available");
        }

        BigtableConfigProvider provider = configProvider.get();
        BigTableClientConfiguration config = provider.getConfiguration(name);
        String projectId = provider.getProjectId();

        if (config == null && LaunchMode.current() == LaunchMode.TEST) {
            LOGGER.infof(
                    "Bigtable client %s created without configuration. We are assuming that you are running tests and using the emulator.",
                    name);
            config = createTestConfiguration();

            String[] hostAndPort = provider.emulatorHost.split(":");
            String host = hostAndPort[0];
            int port = Integer.parseInt(hostAndPort[1]);

            BigtableDataSettings.Builder testClient = BigtableDataSettings.newBuilderForEmulator(host, port)
                    .setProjectId(projectId)
                    .setInstanceId(config.instanceId);

            return BigtableDataClient.create(testClient.build());
        }

        if (config == null || projectId == null) {
            throw new IllegalStateException("Bigtable configuration not found");
        }

        BigtableDataSettings.Builder settings = BigtableDataSettings.newBuilder()
                .setProjectId(projectId)
                .setInstanceId(config.instanceId);

        return BigtableDataClient.create(settings.build());
    }

    private static BigTableClientConfiguration createTestConfiguration() {
        BigTableClientConfiguration config = new BigTableClientConfiguration();
        config.instanceId = "test-instance";
        return config;
    }
}
