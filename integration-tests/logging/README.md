# Quarkiverse - Google Cloud Services - Integration Tests - Google Cloud Logging

This integration test show how to integrate Google Operations Logging.

**WARNING**: It is disabled by default as it needs a Google Cloud Project to run.

Since many of the options of the logging system is static (by configuration) this
test cannot produce all permitations of the logs, but it can be used as a basis for 
manual testing. To run it you need to export `QUARKUS_GOOGLE_CLOUD_PROJECT_ID` (and
optionally `GOOGLE_APPLICATION_CREDENTIALS`):

```shell script
export GOOGLE_APPLICATION_CREDENTIALS=<path here>; \
   export QUARKUS_GOOGLE_CLOUD_PROJECT_ID=<your project>; \
   mvn clean test
```

Do not be alarmed by the `ERROR` in the console: we're deliberately throwing an
error in order to verify the stack trace rendering. 