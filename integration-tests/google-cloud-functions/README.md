# Quarkiverse - Google Cloud Services - Integration Tests - Google Cloud Functions

This integration test show how to integrate Google Cloud Storage with Google Cloud Functions.

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

## Deploying to Google Cloud Functions

First build the application using `mvn clean package`.

Then deploy the function to Google Cloud using:

```
gcloud beta functions deploy quarkus-example-http \
  --entry-point=io.quarkus.gcp.functions.QuarkusHttpFunction \
  --runtime=java11 --trigger-http --source=target/deployment
```

This command will give you as output a `httpsTrigger.url` that points to your function.