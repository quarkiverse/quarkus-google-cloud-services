package io.quarkiverse.googlecloudservices.it.accesstoken;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import com.google.cloud.bigquery.BigQuery;

import io.quarkus.security.Authenticated;

@Path("/datasets")
@Authenticated
public class DatasetsResource {

    @Inject
    BigQuery bigQuery;

    @GET
    public String list() {
        return String.valueOf(bigQuery.listDatasets().iterateAll().iterator().hasNext());
    }
}
