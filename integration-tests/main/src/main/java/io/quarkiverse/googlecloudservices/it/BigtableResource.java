package io.quarkiverse.googlecloudservices.it;

import java.io.IOException;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.cloud.bigtable.admin.v2.BigtableTableAdminSettings;
import com.google.cloud.bigtable.admin.v2.models.CreateTableRequest;
import com.google.cloud.bigtable.admin.v2.stub.BigtableTableAdminStubSettings;
import com.google.cloud.bigtable.admin.v2.stub.EnhancedBigtableTableAdminStub;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.BigtableDataSettings;
import com.google.cloud.bigtable.data.v2.models.Row;
import com.google.cloud.bigtable.data.v2.models.RowCell;
import com.google.cloud.bigtable.data.v2.models.RowMutation;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.quarkiverse.googlecloudservice.bigtable.api.BigtableClient;

@Path("/bigtable")
public class BigtableResource {
    private static final String INSTANCE_ID = "test-instance";
    private static final String TABLE_ID = "test-table";
    private static final String COLUMN_FAMILY_ID = "test-column-family";

    @ConfigProperty(name = "quarkus.google.cloud.project-id")
    String projectId;

    @ConfigProperty(name = "bigtable.authenticated", defaultValue = "true")
    boolean authenticated;

    @ConfigProperty(name = "quarkus.google.cloud.bigtable.emulator-host")
    String emulatorHost;

    @Inject
    @BigtableClient
    BigtableDataClient dataClient;

    @Inject
    CredentialsProvider credentialsProvider;

    @PostConstruct
    void initBigtable() throws IOException {
        if (emulatorHost != null) {
            ManagedChannel channel = ManagedChannelBuilder.forTarget(emulatorHost).usePlaintext().build();

            TransportChannelProvider channelProvider = FixedTransportChannelProvider.create(
                    GrpcTransportChannel.create(channel));
            NoCredentialsProvider credentialsProvider = NoCredentialsProvider.create();

            EnhancedBigtableTableAdminStub stub = EnhancedBigtableTableAdminStub.createEnhanced(
                    BigtableTableAdminStubSettings
                            .newBuilder()
                            .setTransportChannelProvider(channelProvider)
                            .setCredentialsProvider(credentialsProvider)
                            .build());

            try (BigtableTableAdminClient adminClient = BigtableTableAdminClient.create(projectId, INSTANCE_ID, stub)) {
                if (!adminClient.exists(TABLE_ID)) {
                    CreateTableRequest createTableRequest = CreateTableRequest.of(TABLE_ID).addFamily(COLUMN_FAMILY_ID);
                    adminClient.createTable(createTableRequest);
                }
            }
        } else {
            BigtableTableAdminSettings.Builder settings = BigtableTableAdminSettings.newBuilder()
                    .setProjectId(projectId)
                    .setInstanceId(INSTANCE_ID);

            if (authenticated) {
                settings.setCredentialsProvider(credentialsProvider);
            }
            try (BigtableTableAdminClient adminClient = BigtableTableAdminClient.create(settings.build())) {
                if (!adminClient.exists(TABLE_ID)) {
                    CreateTableRequest createTableRequest = CreateTableRequest.of(TABLE_ID).addFamily(COLUMN_FAMILY_ID);
                    adminClient.createTable(createTableRequest);
                }
            }
        }
    }

    @GET
    public String bigtable() throws IOException {
        BigtableDataSettings.Builder settings = BigtableDataSettings.newBuilder()
                .setProjectId(projectId)
                .setInstanceId(INSTANCE_ID);

        if (emulatorHost != null) {
            String[] hostAndPort = emulatorHost.split(":");
            String host = hostAndPort[0];
            int port = Integer.parseInt(hostAndPort[1]);

            settings = BigtableDataSettings.newBuilderForEmulator(host, port)
                    .setProjectId(projectId)
                    .setInstanceId(INSTANCE_ID);
        }

        if (authenticated) {
            settings.setCredentialsProvider(credentialsProvider);
        }
        try (BigtableDataClient dataClient = BigtableDataClient.create(settings.build())) {
            // create a row
            RowMutation rowMutation = RowMutation.create(TABLE_ID, "key1").setCell(COLUMN_FAMILY_ID, "test", "value1");
            this.dataClient.mutateRow(rowMutation);

            Row row = this.dataClient.readRow(TABLE_ID, "key1");
            StringBuilder cells = new StringBuilder();
            for (RowCell cell : row.getCells()) {
                cells.append(String.format(
                        "Family: %s    Qualifier: %s    Value: %s%n",
                        cell.getFamily(), cell.getQualifier().toStringUtf8(), cell.getValue().toStringUtf8()));
            }
            return cells.toString();
        }
    }
}
