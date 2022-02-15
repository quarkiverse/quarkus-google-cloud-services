# Quarkus - Google Cloud Services - Integration Tests - Main

This is the main integration test, it allows to test all Google Cloud services from REST endpoints using a service account authentication.

It contains a test class that can be use to validate all services, this test is not run  by default.

## Running the integration test

If you want to run the integration test, you need to configure a valid GCP project inside the `application.properties`.


Before launching the test, be sure to achieve the following steps to setup all external services.

All the extensions can be tested using the `GoogleServicesResourcesTest`, it needs to be run on a **real Google Cloud** project.
This test is disabled by default as it depends on external services, if you want to run it you need to use the `gcloud` profile : `mvn test -Pgcloud`.

### Bigtable

To test Bigtable you first need to create a Bigtable instance named `test-instane`

You can create one with `gcloud`:

```
gcloud bigtable instances create test-instance \
    --cluster=test-cluster \
    --cluster-zone=europe-west1-b \
    --display-name=Test
```

As Bigtable mandates the usage of the `GOOGLE_APPLICATION_CREDENTIALS` environment variable to define its credentials, 
you need to set this one instead of relying on the `quarkus.google.cloud.service-account-location` property. 

```
export GOOGLE_APPLICATION_CREDENTIALS=<your-service-account-file>
```

### PubSub

To test PubSub you first need to create a topic named `test-topic`

You can create one with `gcloud`:

```
gcloud pubsub topics create test-topic
```

As PubSub mandates the usage of the `GOOGLE_APPLICATION_CREDENTIALS` environment variable to define its credentials, 
you need to set this one instead of relying on the `quarkus.google.cloud.service-account-location` property. 

```
export GOOGLE_APPLICATION_CREDENTIALS=<your-service-account-file>
```

### Secret Manager

To test Secret Manager you first need to create a secret;

You can create one with `gcloud`:

```
gcloud secrets create test-secret --replication-policy="automatic"
printf "integration-test-secret" | gcloud secrets versions add integration-test --data-file=-
```

### Spanner

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

### Storage

To test Storage you first need to create a bucket named `quarkus-hello` then upload a file `hello.txt` in it.
This file will be read by the test and returned from the endpoint.

You can use `gsutil`:

```
gsutil mb gs://quarkus-hello
echo "Hello World!" > hello.txt
gsutil cp hello.txt gs://my-bucket
```

## Automated tests

There exist automated test that use `gcloud emulator`, it allows to test some on the extensions easily and is launched by the CI.

Those tests are launch via `mvn test`.