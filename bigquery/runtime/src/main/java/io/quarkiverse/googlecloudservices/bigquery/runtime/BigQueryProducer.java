package io.quarkiverse.googlecloudservices.bigquery.runtime;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;

import io.quarkiverse.googlecloudservices.common.GcpBootstrapConfiguration;
import io.quarkiverse.googlecloudservices.common.GcpConfigHolder;

@ApplicationScoped
public class BigQueryProducer {

    @Inject
    GoogleCredentials googleCredentials;

    @Inject
    GcpConfigHolder gcpConfigHolder;

    @Produces
    @Singleton
    @Default
    public BigQuery bigQuery() throws IOException {
        GcpBootstrapConfiguration gcpConfiguration = gcpConfigHolder.getBootstrapConfig();
        return BigQueryOptions.newBuilder().setCredentials(googleCredentials)
                .setProjectId(gcpConfiguration.projectId.orElse(null))
                .build()
                .getService();
    }
}
