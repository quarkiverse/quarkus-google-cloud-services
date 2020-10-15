# Quarkiverse - Google Cloud Services - Integration Tests - Google App Engine

This integration test show how to integrate Google Cloud Storage with Google App Engine.

**WARNING**: It is disabled by default as it needs a Google Cloud Project to run.

This test needs a bucket named `quarkus-hello` with a file `hello.txt` in it.
This file will be read by the test and returned from the endpoint.

You can use `gsutil` to create them:

```
gsutil mb gs://quarkus-hello
echo "Hello World!" > hello.txt
gsutil cp hello.txt gs://my-bucket
```

Then you need to configure your Google Cloud project inside the `application.properties`:
```
quarkus.google.cloud.project-id=<my-propject-id>
```

## Deploying to Google App Engine

NOTE: __There are multiple ways to build and deploy an application to App Engine, in this integration test we decided to build an uber-jar locally and deploy it to App Engine.
You can let App Engine build the jar for you but as this integration test uses snapshot version of the library that are not published this cannot work.__

First build the application using `mvn clean package`, it will build an uber jar as configured inside the `application.properties`.

Then deploy the uber jar to Google Cloud using:

```
gcloud app deploy target/quarkus-google-cloud-services-app-engine-it-*-runner.jar
```

This command will give you an output as follow where you could find the URL of the service:
```
descriptor:      [quarkiverse-google-cloud-services/integration-tests/app-engine/target/quarkus-google-cloud-services-app-engine-it-0.2.0-SNAPSHOT-runner.jar]
source:          [quarkiverse-google-cloud-services/integration-tests/app-engine/target]
target project:  [my-propject-id]
target service:  [default]
target version:  [20201015t104717]
target url:      [https://my-propject-id.ew.r.appspot.com]

```

The endpoint will then be available at https://my-propject-id.ew.r.appspot.com/storage.