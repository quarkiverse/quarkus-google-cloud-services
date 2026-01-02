package io.quarkiverse.googlecloudservices.it;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.google.cloud.spanner.*;

@Path("/spanner")
public class SpannerResource {
    private static final Logger LOG = Logger.getLogger(SpannerResource.class);

    @Inject
    Spanner spanner;

    @ConfigProperty(name = "quarkus.google.cloud.project-id")
    String projectId;

    @POST
    public void createDatabase() throws ExecutionException, InterruptedException, TimeoutException {
        // create the instance
        spanner.getInstanceAdminClient().createInstance(
                InstanceInfo.newBuilder(InstanceId.of(projectId, "test-instance"))
                        .setInstanceConfigId(InstanceConfigId.of(projectId, "test-config")).build())
                .get(1, TimeUnit.SECONDS);

        // create the database and the table
        spanner.getDatabaseAdminClient()
                .createDatabase("test-instance", "test-database", List.of(
                        "CREATE TABLE Singers ( SingerId INT64 NOT NULL, FirstName STRING(1024), LastName STRING(1024), SingerInfo BYTES(MAX) ) PRIMARY KEY (SingerId)"))
                .get(1, TimeUnit.SECONDS);
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String spanner() {
        DatabaseId id = DatabaseId.of(projectId, "test-instance", "test-database");
        DatabaseClient dbClient = spanner.getDatabaseClient(id);

        // Insert 4 singer records
        dbClient.readWriteTransaction().run(transaction -> {
            String sql = "INSERT INTO Singers (SingerId, FirstName, LastName) VALUES "
                    + "(12, 'Melissa', 'Garcia'), "
                    + "(13, 'Russell', 'Morales'), "
                    + "(14, 'Jacqueline', 'Long'), "
                    + "(15, 'Dylan', 'Shaw')";
            long rowCount = transaction.executeUpdate(Statement.of(sql));
            LOG.infov("{0} records inserted.", rowCount);
            return null;
        });

        // read them
        try (ResultSet resultSet = dbClient.singleUse() // Execute a single read or query against Cloud Spanner.
                .executeQuery(Statement.of("SELECT SingerId, FirstName, LastName FROM Singers"))) {
            StringBuilder builder = new StringBuilder();
            while (resultSet.next()) {
                builder.append(resultSet.getLong(0)).append(' ').append(resultSet.getString(1)).append(' ')
                        .append(resultSet.getString(2)).append('\n');
            }
            return builder.toString();
        }
    }

}
