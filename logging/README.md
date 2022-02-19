# Quarkiverse - Google Cloud Services - Logging

This extension allows you to integrate your logs directly into GCP Operations logging, including the ability to set
resource labels and log record labels, include trace ID and to use structured logging. 

## Bootstrapping the project

First, we need a new project. Create a new project with the following command (replace the version placeholders with the correct ones):

```shell script
mvn io.quarkus:quarkus-maven-plugin:${quarkusVersion}:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=logging-quickstart \
    -Dextensions="resteasy-jackson,io.quarkiverse.googlecloudservices:quarkus-google-cloud-logging:${googleCloudServicesVersion}"
cd logging-quickstart
```

This command generates a Maven project, importing the Google Cloud Logging extension.

If you already have your Quarkus project configured, you can add the `quarkus-google-cloud-logging` extension to your project by running the following command in your project base directory:
```shell script
./mvnw quarkus:add-extension -Dextensions="io.quarkiverse.googlecloudservices:quarkus-google-cloud-logging:${googleCloudServicesVersion}"
```

This will add the following to your pom.xml:

```xml
<dependency>
    <groupId>io.quarkiverse.googlecloudservices</groupId>
    <artifactId>quarkus-google-cloud-logging</artifactId>
    <version>${googleCloudServicesVersion}</version>
</dependency>
```

## How-to: Log
When configured, all that is needed is to log data as you wish. You can use `JUL`, `jboss-logging` or `Slf4j`, or even GCP logging directly. You can also 
inject the logger to use, so for example: 

```java
package mypackage;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.logging.Logger;

@Path("/logging")
public class LoggingResource {

    @Inject
    Logger logger;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String tryLog() {
        logger.info("Greeting the World");
        return "Hello World!";
    }
}
```

##  Configuration

### Basic
Currently the logging system uses a single GCP Operations log and this is a mandatory configuration parameter. 

* `quarkus.google.cloud.logging.default-log`
* `quarkus.google.cloud.project-id`

If you which to disable the logging to GCP you can do so: 

* `quarkus.google.cloud.logging.enabled=false`

This is useful for testing, not to write to Google Operations during development: 

* `%test.quarkus.google.cloud.logging.enabled=false`

### Formatting
This logging system can either use a standard text based format, Elastic Common Schema, or a completely custom schema.    

* `quarkus.google.cloud.logging.format=[TEXT|JSON]`

#### Structured Logging
By setting the logging `format` to `JSON` this library will default to ECS format. As local record parameters and MDC values are not
official part of the ECS specification, these default to `parameters`  and `mdc` respectively, but this can be configured (see below).

* `quarkus.google.cloud.logging.structured.stack-trace.included=[true|false]` - Should stack traces be included? Default to `true`
* `quarkus.google.cloud.logging.structured.stack-trace.rendering=[array|string]` - Render stack traces as a strings or an array of objects, defaults to `string`
* `quarkus.google.cloud.logging.structured.stack-trace.element-rendering=[string|object]` - How to render each stack trace element, defaults to `string`
* `quarkus.google.cloud.logging.structured.parameters.included=[true|false]` - Should log record parameters be included? Defaults to `true`
* `quarkus.google.cloud.logging.structured.parameters.field-name` - This is the field name to use in ESC for parameters, defaults to `parameters`
* `quarkus.google.cloud.logging.structured.mdc.included=[true|false]` - Should the MDC values be included? Defaults to `true`
* `quarkus.google.cloud.logging.structured.mdc.field-name` - This is the field name to use in ESC for MDC values, defaults to `mdc`

### Resource type and labels
You can configure the resource type and the resource labels. This is used by GCP operations as dimensions
when analysing logs. Please refer to the [GCP documentation](https://cloud.google.com/logging/docs/api/v2/resource-list#resource-types) 
for the allowable resource types and their associated labels. 

* `quarkus.google.cloud.logging.resource.type` - The type string, defaults to `generic_node`
* `quarkus.google.cloud.logging.resource.label.<string>` - This is a string map with key/value pairs

### Log record labels
You can configure default labels for each log record:

* `quarkus.google.cloud.logging.default-label.<string>` - This is a string map with key/value pairs

You can also extract custom labels per record by binding a `LabelExtractor` to the CDI context, e.g.: 

```java
package mypackage;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logmanager.ExtLogRecord;

import io.quarkiverse.googlecloudservices.logging.runtime.LabelExtractor;

@ApplicationScoped
public class TestLabelExtractor implements LabelExtractor {

    public Map<String, String> extract(ExtLogRecord record) {
        if (record.getParameters() == null) {
            return null;
        } else {
            // do something smart here
        }
    }
}
```

### Tracing
In order to include trace and span ID in the log you must bind a `TraceInfoExtractor` to the CDI context, e.g.:

```java
package mypackage;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logmanager.ExtLogRecord;

import io.quarkiverse.googlecloudservices.logging.runtime.TraceInfo;
import io.quarkiverse.googlecloudservices.logging.runtime.TraceInfoExtractor;

@ApplicationScoped
public class TestTraceInfoExtractor implements TraceInfoExtractor {

    @Override
    public TraceInfo extract(ExtLogRecord record) {
        return new TraceInfo(/* something, something */);
    }
}
```

This will include the trace and span ID in the ESC logging if they are not null. By default this
will also be set on the GCP operations log entry as a link to GCP operations tracing, but this is
configurable: 

* `quarkus.google.cloud.logging.gcp-tracing.enabled=[true|false]`

# Injecting GCP Logging
You can inject a `Logging` instance directly. If you do, the configuration for the project to use,
still apply. 

If you want the configuration for default log, resource type and labels, and default labels, you can
inject a `WriteOptionsHolder` which contains an array of default write options as configured. 

```java
[...]

@Inject
Logging gcpLogging;

@Inject
WriteOptionsHolder defaultOptions;

public void log(String s) {
    gcp.write(ImmutableList.of(LogEntry.newBuilder(Payload.StringPayload.of(s))
        .setSeverity(Severity.DEBUG)
        .setTimestamp(Instant.now())
        .build())
    , defaultOptions.getOptions());
}

[...]
```