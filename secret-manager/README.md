# Quarkiverse - Google Cloud Services - Secret Manager

This extension allows to use the Google Cloud Platform Secret Manager client library inside your Quarkus application. The current implementation is focused on _accessing_ the secrets, but extending the implementation into also allowing to create or edit a secret might be a natural next step.

## Bootstrapping the project

First, we need a new project. Create a new project with the following command (replace the version placeholders with the correct ones):

```shell script
mvn io.quarkus:quarkus-maven-plugin:${quarkusVersion}:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=secretmanager-quickstart \
    -Dextensions="resteasy-jackson,io.quarkiverse.googlecloudservices:quarkus-google-cloud-secret-manager:${googleCloudServicesVersion}"
cd secretmanager-quickstart
```

This command generates a Maven project, importing the Google Cloud Secret Manager extension.

If you already have your Quarkus project configured, you can add the `quarkus-google-cloud-secret-manager` extension to your project by running the following command in your project base directory:
```shell script
./mvnw quarkus:add-extension -Dextensions="io.quarkiverse.googlecloudservices:quarkus-google-cloud-secret-manager:${googleCloudServicesVersion}"
```

This will add the following to your pom.xml:

```xml
<dependency>
    <groupId>io.quarkiverse.googlecloudservices</groupId>
    <artifactId>quarkus-google-cloud-secret-manager</artifactId>
    <version>${googleCloudServicesVersion}</version>
</dependency>
```

## Some example
This is an example fetching a single secret from GCP Secret Manager.

First, you'll have to create the secret in the GCP Secret Manager, as described in Google's documentation at https://cloud.google.com/secret-manager/docs/creating-and-accessing-secrets.

The following `gcloud` commands will create a secret named `test-secret` with the value `s3cr3t` in it.

```shell
gcloud secrets create test-secret --replication-policy="automatic"
printf "s3cr3t" | gcloud secrets versions add integration-test --data-file=-
```

The _secretName_ parameter in the examples below refer to the name you give your secret, whereas the injected _secretManagerProjectId_ refers to the name of your project on GCP.

```java
import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@RequestScoped
public class GCPSecretManager {

    @ConfigProperty(name = "quarkus.google.cloud.project-id")
    String projectId;

    public String getSecretFromSecretManager(String secretName) throws IOException {
        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            SecretVersionName secretVersionName = SecretVersionName.of(projectId, "test-secret", "latest");
            AccessSecretVersionResponse response = client.accessSecretVersion(secretVersionName);
            return response.getPayload().getData().toStringUtf8();
        }
    }
}
```