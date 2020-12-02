# Quarkiverse - Google Cloud Services - Spanner

This extension allows to inject a `com.google.cloud.spanner.Spanner` object inside your Quarkus application.

## Bootstrapping the project

First, we need a new project. Create a new project with the following command (replace the version placeholders with the correct ones):

```shell script
mvn io.quarkus:quarkus-maven-plugin:<quarkusVersion>:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=spanner-quickstart \
    -Dextensions="resteasy-jackson,io.quarkiverse.googlecloudservices:quarkus-google-cloud-spanner:${googleCloudServicesVersion}"
cd spanner-quickstart
```

This command generates a Maven project, importing the Google Cloud Spanner extension.

If you already have your Quarkus project configured, you can add the `quarkus-google-cloud-spanner` extension to your project by running the following command in your project base directory:
```shell script
./mvnw quarkus:add-extension -Dextensions="io.quarkiverse.googlecloudservices:quarkus-google-cloud-spanner:${googleCloudServicesVersion}"
```

This will add the following to your pom.xml:

```xml
<dependency>
    <groupId>io.quarkiverse.googlecloudservices</groupId>
    <artifactId>quarkus-google-cloud-spanner</artifactId>
    <version>${googleCloudServicesVersion}</version>
</dependency>
```

## Preparatory steps

To test Spanner you first need to have a running Spanner cluster named `test-instance`.

You can create one with `gcloud`:
```
gcloud spanner instances create test-instance --config=regional-us-central1 \
    --description="Test Instance" --nodes=1
```

Then you need a database named `test-database`.

You can create one with `gcloud`:
```
gcloud spanner databases create test-database --instance test-instance
```

And finally you need to create a table named `Singers`.

You can do it with `gcloud`:
```
gcloud spanner databases ddl update test-database --instance test-instance \
  --ddl='CREATE TABLE Singers ( SingerId INT64 NOT NULL, FirstName STRING(1024), LastName STRING(1024), SingerInfo BYTES(MAX) ) PRIMARY KEY (SingerId)'
```

## Some example

This is an example usage of the extension: we create a REST resource with a single endpoint that inserts four singers inside the `Singers` table,
then reads the all table and returns its elements. 

```java
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.google.cloud.spanner.*;

@Path("/spanner")
public class SpannerResource {
    private static final Logger LOG = Logger.getLogger(SpannerResource.class);

    @Inject
    Spanner spanner;// Inject Spanner

    @ConfigProperty(name = "quarkus.google.cloud.project-id")
    String projectId;// Inject the project name

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String spanner() {
        // Create a database client
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

        // Read them
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
```
