# Quarkiverse - Google Cloud Services - Secret Manager

This extension allows to inject the Google Cloud Platform Secret Manager client library inside your Quarkus application.

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
This is an example fetching a single secret from GCP Secret Manager. The example is in Kotlin, but the code can be transformed into Java as well.

```kotlin
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient
import com.google.cloud.secretmanager.v1.SecretVersionName
import org.eclipse.microprofile.config.inject.ConfigProperty
import javax.enterprise.context.RequestScoped

@RequestScoped
class GCPSecretManager {

    @ConfigProperty(name = "secretManagerProjectId", defaultValue = "")
    lateinit var secretManagerProjectId: String
    
    fun getSecretFromSecretManager(secretName: String): String {
        val secretVersionName = SecretVersionName.of(secretManagerProjectId, secretName, "latest")
        val client = SecretManagerServiceClient.create()
        return client.accessSecretVersion(secretVersionName).payload.data.toStringUtf8()
    }
}
```