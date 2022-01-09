# Quarkiverse - Google Cloud Services - Translate

This extension allows to inject a `com.google.cloud.translate.Translate` object inside your Quarkus application.

## Bootstrapping the project

First, we need a new project. Create a new project with the following command (replace the version placeholders with the correct ones):

```shell script
mvn io.quarkus:quarkus-maven-plugin:<quarkusVersion>:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=bigquery-quickstart \
    -Dextensions="resteasy-jackson,io.quarkiverse.googlecloudservices:quarkus-google-cloud-translate:${googleCloudServicesVersion}"
cd translate-quickstart
```

This command generates a Maven project, importing the Google Cloud Translate extension.

If you already have your Quarkus project configured, you can add the `quarkus-google-cloud-translate` extension to your project by running the following command in your project base directory:
```shell script
./mvnw quarkus:add-extension -Dextensions="io.quarkiverse.googlecloudservices:quarkus-google-cloud-translate:${googleCloudServicesVersion}"
```

This will add the following to your pom.xml:

```xml
<dependency>
    <groupId>io.quarkiverse.googlecloudservices</groupId>
    <artifactId>quarkus-google-cloud-translate</artifactId>
    <version>${googleCloudServicesVersion}</version>
</dependency>
```

## Some example

This is an example usage of the extension: we create a REST resource with a single endpoint that gets the translation for `Quarkus says hello world!` sentence in french and returns it.

```java
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.Translate.TranslateOption;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;

@Path("/translate")
public class TranslateResource {

    @Inject
    Translate translate;// Inject Translate

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String translate() {

        Translation translation =
                translate.translate(
                        "Quarkus says hello world!",
                        TranslateOption.sourceLanguage("en"),
                        TranslateOption.targetLanguage("fr");

        return translation.getTranslatedText(); // Return its content
    }

}
```
