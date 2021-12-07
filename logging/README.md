# Quarkiverse - Google Cloud Services - Logging

This extension allows to inject a `com.google.cloud.storage.Storage` object inside your Quarkus application.

## Bootstrapping the project

First, we need a new project. Create a new project with the following command (replace the version placeholders with the correct ones):

```shell script
mvn io.quarkus:quarkus-maven-plugin:<quarkusVersion>:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=bigquery-quickstart \
    -Dextensions="resteasy-jackson,io.quarkiverse.googlecloudservices:quarkus-google-cloud-storage:${googleCloudServicesVersion}"
cd storage-quickstart
```

This command generates a Maven project, importing the Google Cloud Storage extension.

If you already have your Quarkus project configured, you can add the `quarkus-google-cloud-storage` extension to your project by running the following command in your project base directory:
```shell script
./mvnw quarkus:add-extension -Dextensions="io.quarkiverse.googlecloudservices:quarkus-google-cloud-storage:${googleCloudServicesVersion}"
```

This will add the following to your pom.xml:

```xml
<dependency>
    <groupId>io.quarkiverse.googlecloudservices</groupId>
    <artifactId>quarkus-google-cloud-storage</artifactId>
    <version>${googleCloudServicesVersion}</version>
</dependency>
```

## Preparatory steps

To test Storage you first need to create a bucket named `quarkus-hello` then upload a file `hello.txt` in it.
This file will be read by the test and returned from the endpoint.

You can use `gsutil`:

```
gsutil mb gs://quarkus-hello
echo "Hello World!" > hello.txt
gsutil cp hello.txt gs://my-bucket
```

## Some example

This is an example usage of the extension: we create a REST resource with a single endpoint that gets the `hello.txt` object
from the `quarkus-hello` bucket and returns its content.

```java
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;

@Path("/storage")
public class StorageResource {

    @Inject
    Storage storage;// Inject Storage

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String storage() {
        Bucket bucket = storage.get("quarkus-hello");// Get the bucket
        Blob blob = bucket.get("hello.txt"); // Get the object
        return new String(blob.getContent()); // Return its content
    }

}
```
