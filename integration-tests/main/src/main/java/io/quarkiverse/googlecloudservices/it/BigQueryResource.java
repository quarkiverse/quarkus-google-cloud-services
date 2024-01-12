package io.quarkiverse.googlecloudservices.it;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobId;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.google.cloud.bigquery.storage.v1.*;
import com.google.protobuf.Descriptors;

@Path("/bigquery")
public class BigQueryResource {
    @Inject
    BigQuery bigquery;

    @Inject
    BigQueryWriteClient bigQueryWriteClient;

    @ConfigProperty(name = "quarkus.google.cloud.project-id")
    String projectId;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String bigquery() throws InterruptedException {
        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(// Define a new Job with the query
                "SELECT "
                        + "CONCAT('https://stackoverflow.com/questions/', CAST(id as STRING)) as url, view_count "
                        + "FROM `bigquery-public-data.stackoverflow.posts_questions` "
                        + "WHERE tags like '%google-bigquery%' ORDER BY favorite_count DESC LIMIT 10")
                .setUseLegacySql(false)
                .build();

        // Create a job ID so that we can safely retry.
        JobId jobId = JobId.of(UUID.randomUUID().toString());
        Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());

        // Wait for the query to complete.
        queryJob = queryJob.waitFor();

        // Check for errors
        if (queryJob == null) {
            throw new RuntimeException("Job no longer exists");
        } else if (queryJob.getStatus().getError() != null) {
            throw new RuntimeException(queryJob.getStatus().getError().toString());
        }

        // Get the results and return them
        TableResult result = queryJob.getQueryResults();
        return StreamSupport.stream(result.iterateAll().spliterator(), false)
                .map(row -> row.get("url").getStringValue() + " - " + row.get("view_count").getLongValue() + "\n")
                .collect(Collectors.joining());
    }

    @GET
    @Path("/writeClient")
    @Produces(MediaType.TEXT_PLAIN)
    public String bigQueryWriteClient()
            throws InterruptedException, Descriptors.DescriptorValidationException, IOException, ExecutionException {
        TableName parentTable = TableName.of(projectId, "testdataset", "testtable");
        WriteStream stream = WriteStream.newBuilder().setType(WriteStream.Type.PENDING).build();
        CreateWriteStreamRequest createWriteStreamRequest = CreateWriteStreamRequest.newBuilder()
                .setParent(parentTable.toString())
                .setWriteStream(stream)
                .build();
        WriteStream writeStream = bigQueryWriteClient.createWriteStream(createWriteStreamRequest);
        try (JsonStreamWriter streamWriter = JsonStreamWriter.newBuilder(writeStream.getName(), writeStream.getTableSchema())
                .build()) {
            long offset = 0;
            for (int i = 0; i < 2; i++) {
                // Create a JSON object that is compatible with the table schema.
                JSONArray jsonArr = new JSONArray();
                for (int j = 0; j < 10; j++) {
                    JSONObject record = new JSONObject();
                    record.put("col1", String.format("batch-record %03d-%03d", i, j));
                    jsonArr.put(record);
                }
                streamWriter.append(jsonArr, offset).get();
                offset += jsonArr.length();
            }
        }

        return "OK";
    }
}
