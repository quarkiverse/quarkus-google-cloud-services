package io.quarkiverse.googlecloudservices.bigtable.runtime;

import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.BigtableDataSettings;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.runtime.LaunchMode;

public class BigtableClients {

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
            return null;
        }

        if (config == null || projectId == null) {
            throw new IllegalStateException("Bigtable configuration not found");
        }

        BigtableDataSettings.Builder settings = BigtableDataSettings.newBuilder()
                .setProjectId(projectId)
                .setInstanceId(config.instanceId);

        return BigtableDataClient.create(settings.build());
    }
}
