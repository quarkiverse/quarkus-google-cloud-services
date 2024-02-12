package io.quarkiverse.googlecloudservices.bigquery.runtime;

import java.io.IOException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.storage.v1.BigQueryWriteClient;
import com.google.cloud.bigquery.storage.v1.BigQueryWriteSettings;

import io.quarkiverse.googlecloudservices.common.GcpBootstrapConfiguration;
import io.quarkiverse.googlecloudservices.common.GcpConfigHolder;

@ApplicationScoped
public class BigQueryProducer {

    @Inject
    Credentials googleCredentials;

    @Inject
    CredentialsProvider credentialsProvider;

    @Inject
    GcpConfigHolder gcpConfigHolder;

    @Produces
    @Singleton
    @Default
    public BigQuery bigQuery() {
        GcpBootstrapConfiguration gcpConfiguration = gcpConfigHolder.getBootstrapConfig();
        return BigQueryOptions.newBuilder().setCredentials(googleCredentials)
                .setProjectId(gcpConfiguration.projectId().orElse(null))
                .build()
                .getService();
    }

    @Produces
    @Singleton
    @Default
    public BigQueryWriteClient bigQueryWriteClient() throws IOException {
        GcpBootstrapConfiguration gcpConfiguration = gcpConfigHolder.getBootstrapConfig();
        BigQueryWriteSettings bigQueryWriteSettings = BigQueryWriteSettings.newBuilder()
                .setCredentialsProvider(credentialsProvider)
                .setQuotaProjectId(gcpConfiguration.projectId().orElse(null))
                .build();
        return BigQueryWriteClient.create(bigQueryWriteSettings);
    }
}
