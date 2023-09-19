package io.quarkiverse.googlecloudservices.bigtable.runtime;

import java.util.Map;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

public class BigtableConfigProvider {

    @Inject
    BigtableConfiguration config;

    @ConfigProperty(name = "quarkus.google.cloud.project-id")
    String projectId;

    public BigTableClientConfiguration getConfiguration(String name) {
        Map<String, BigTableClientConfiguration> clients = config.clients;
        if (clients == null) {
            return null;
        } else {
            return clients.get(name);
        }
    }

    public String getProjectId() {
        return projectId;
    }
}
